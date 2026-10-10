package com.stayease.controller;

import com.stayease.model.Hotel;
import com.stayease.model.Room;
import com.stayease.model.User;
import com.stayease.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/manager/hotels/{hotelId}/rooms")
@RequiredArgsConstructor
public class RoomController
{
    private final RoomService roomService;
    private final HotelService hotelService;
    private final UserService userService;
    private final ManagerService managerService;
    private final ImageService imageService;

    @ModelAttribute("user")
    public User getUser(Authentication authentication)
    {
        return userService.getUserByUserEmail(authentication.getName());
    }

    @ModelAttribute("currentPage")
    public String currentPage()
    {
        return "managerHotels";
    }

    @GetMapping
    public String rooms(@ModelAttribute("user") User user, @PathVariable String hotelId, Model model)
    {
        String managerId = user.getUserId();

        if (!managerService.isHotelOwnedByManager(hotelId, managerId))
        {
            return "redirect:/manager/dashboard";
        }

        Hotel hotel = hotelService.getHotelById(hotelId);

        model.addAttribute("hotel", hotel);
        model.addAttribute("rooms", roomService.getHotelRooms(hotelId));

        return "manager/rooms";
    }

    @GetMapping("/add")
    public String addRoomPage(@ModelAttribute("user") User user, @PathVariable String hotelId, Model model)
    {
        String managerId = user.getUserId();

        if (!managerService.isHotelOwnedByManager(hotelId, managerId))
        {
            return "redirect:/manager/dashboard";
        }

        model.addAttribute("room", new Room());
        model.addAttribute("hotelId", hotelId);

        return "manager/add-room";
    }


    @GetMapping("/edit/{roomId}")
    public String editRoomPage(@ModelAttribute("user") User user, @PathVariable String hotelId, @PathVariable String roomId, Model model)
    {
        String managerId = user.getUserId();

        if (!managerService.isHotelOwnedByManager(hotelId, managerId))
        {
            return "redirect:/manager/dashboard";
        }

        Room room = roomService.getRoomById(roomId);

        if (room == null || !hotelId.equals(room.getHotelId()))
        {
            return "redirect:/manager/hotels";
        }

        model.addAttribute("room", room);
        model.addAttribute("hotelId", hotelId);

        return "manager/edit-room";
    }

    @PostMapping("/add")
    public String addRoom(
            @ModelAttribute("user") User user,
            @PathVariable String hotelId,
            @Valid @ModelAttribute("room") Room room,
            BindingResult bindingResult,
            @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles)
    {
        String managerId = user.getUserId();

        if (!managerService.isHotelOwnedByManager(hotelId, managerId))
        {
            return "redirect:/manager/dashboard";
        }

        if (bindingResult.hasErrors())
        {
            return "manager/add-room";
        }

        List<String> imagePaths = imageService.saveHRoomImages(imageFiles);
        room.setRoomImages(imagePaths);

        if (!imagePaths.isEmpty())
        {
            room.setRoomCoverImage(imagePaths.getFirst());
        }

        roomService.createRoom(room, hotelId);

        return "redirect:/manager/hotels/" + hotelId + "/rooms";
    }

    @PostMapping("/edit/{roomId}")
    public String updateRoom(
            @ModelAttribute("user") User user,
            @PathVariable String hotelId,
            @PathVariable String roomId,
            @Valid @ModelAttribute Room room,
            BindingResult bindingResult,
            Model model)
    {
        String managerId = user.getUserId();

        if (!managerService.isHotelOwnedByManager(hotelId, managerId))
        {
            return "redirect:/manager/dashboard";
        }

        Room existingRoom = roomService.getRoomById(roomId);

        if (existingRoom == null || !hotelId.equals(existingRoom.getHotelId()))
        {
            return "redirect:/manager/hotels";
        }

        if (bindingResult.hasErrors())
        {
            model.addAttribute("roomId", roomId);
            model.addAttribute("hotelId", hotelId);

            return "manager/edit-room";
        }

        roomService.updateRoom(roomId, room);

        return "redirect:/manager/hotels/" + hotelId + "/rooms";
    }

    @PostMapping("/delete/{roomId}")
    public String deleteRoom(@ModelAttribute("user") User user, @PathVariable String hotelId, @PathVariable String roomId)
    {
        String managerId = user.getUserId();

        if (!managerService.isHotelOwnedByManager(hotelId, managerId))
        {
            return "redirect:/manager/dashboard";
        }

        Room room = roomService.getRoomById(roomId);

        if (!room.getHotelId().equals(hotelId))
        {
            return "redirect:/manager/dashboard";
        }

        roomService.deleteRoom(roomId);

        return "redirect:/manager/hotels/" + hotelId + "/rooms";
    }

    @PostMapping("/edit/image/{roomId}")
    public String updateRoomImage(
            @ModelAttribute("user") User user,
            @PathVariable String hotelId,
            @PathVariable String roomId,
            @ModelAttribute Room room,
            @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles)
    {
        String managerId = user.getUserId();

        if (!managerService.isHotelOwnedByManager(hotelId, managerId))
        {
            return "redirect:/manager/dashboard";
        }

        Room existingRoom = roomService.getRoomById(roomId);

        if (!existingRoom.getHotelId().equals(hotelId))
        {
            return "redirect:/manager/dashboard";
        }

        List<String> images = new ArrayList<>();

        if (existingRoom.getRoomImages() != null)
        {
            images.addAll(existingRoom.getRoomImages());
        }

        List<String> newImages = imageService.saveHRoomImages(imageFiles);
        images.addAll(newImages);

        room.setRoomImages(images);
        room.setRoomCoverImage(existingRoom.getRoomCoverImage());

        roomService.addRoomImage(roomId, room);

        return "redirect:/manager/hotels/" + hotelId + "/rooms/edit/" + roomId;
    }

    @PostMapping("/images/cover/{roomId}")
    public String setRoomCoverImage(
            @PathVariable String hotelId,
            @PathVariable String roomId,
            @RequestParam String image)
    {
        roomService.setRoomCoverImage(roomId, image);

        return "redirect:/manager/hotels/" + hotelId + "/rooms/edit/" + roomId;
    }

    @PostMapping("/images/delete/{roomId}")
    public String deleteHotelImage(
            @PathVariable String hotelId,
            @PathVariable String roomId,
            @RequestParam String image)
    {
        roomService.deleteRoomImage(roomId, image);

        return "redirect:/manager/hotels/" + hotelId + "/rooms/edit/" + roomId;
    }
}