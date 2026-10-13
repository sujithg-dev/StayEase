package com.stayease.controller;

import com.stayease.config.SecurityConfig;
import com.stayease.model.Hotel;
import com.stayease.model.Room;
import com.stayease.model.User;
import com.stayease.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class RoomControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    private HotelService hotelService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ManagerService managerService;

    @MockitoBean
    private ImageService imageService;

    private Room room;
    private Hotel hotel;

    @BeforeEach
    void setUp()
    {
        User user = new User();
        user.setUserId("manager-1");
        user.setUserName("Test Manager");
        user.setUserEmail("manager@example.com");

        room = new Room();
        room.setHotelId("hotel-1");

        hotel = new Hotel();

        when(userService.getUserByUserEmail("manager@example.com")).thenReturn(user);
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void rooms_whenManagerOwnsHotel_returnsRoomsPage() throws Exception
    {
        when(managerService.isHotelOwnedByManager("hotel-1", "manager-1")).thenReturn(true);
        when(hotelService.getHotelById("hotel-1")).thenReturn(hotel);
        when(roomService.getHotelRooms("hotel-1")).thenReturn(List.of(room));

        mockMvc.perform(get("/manager/hotels/hotel-1/rooms"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/rooms"))
                .andExpect(model().attributeExists("hotel", "rooms", "user", "currentPage"));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void rooms_whenManagerDoesNotOwnHotel_redirectsToDashboard() throws Exception
    {
        when(managerService.isHotelOwnedByManager("hotel-1", "manager-1")).thenReturn(false);

        mockMvc.perform(get("/manager/hotels/hotel-1/rooms"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/dashboard"));

        verifyNoInteractions(roomService);
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void addRoomPage_whenManagerOwnsHotel_returnsAddRoomPage() throws Exception
    {
        when(managerService.isHotelOwnedByManager("hotel-1", "manager-1")).thenReturn(true);

        mockMvc.perform(get("/manager/hotels/hotel-1/rooms/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/add-room"))
                .andExpect(model().attributeExists("room", "hotelId"));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void addRoomPage_whenManagerDoesNotOwnHotel_redirectsToDashboard() throws Exception
    {
        when(managerService.isHotelOwnedByManager("hotel-1", "manager-1")).thenReturn(false);

        mockMvc.perform(get("/manager/hotels/hotel-1/rooms/add"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/dashboard"));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void editRoomPage_whenRoomBelongsToHotel_returnsEditPage() throws Exception
    {
        when(managerService.isHotelOwnedByManager("hotel-1", "manager-1")).thenReturn(true);
        when(roomService.getRoomById("room-1")).thenReturn(room);

        mockMvc.perform(get("/manager/hotels/hotel-1/rooms/edit/room-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/edit-room"))
                .andExpect(model().attributeExists("room", "hotelId"));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void editRoomPage_whenRoomDoesNotBelongToHotel_redirectsToHotels() throws Exception
    {
        when(managerService.isHotelOwnedByManager("hotel-1", "manager-1")).thenReturn(true);

        Room otherRoom = new Room();
        otherRoom.setHotelId("hotel-2");
        when(roomService.getRoomById("room-1")).thenReturn(otherRoom);

        mockMvc.perform(get("/manager/hotels/hotel-1/rooms/edit/room-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/hotels"));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void addRoom_whenManagerDoesNotOwnHotel_doesNotCreateRoom() throws Exception
    {
        when(managerService.isHotelOwnedByManager("hotel-1", "manager-1")).thenReturn(false);

        mockMvc.perform(post("/manager/hotels/hotel-1/rooms/add")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/dashboard"));

        verify(roomService, never()).createRoom(any(), anyString());
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void addRoom_whenValid_createsRoomAndRedirects() throws Exception
    {
        when(managerService.isHotelOwnedByManager("hotel-1", "manager-1")).thenReturn(true);
        when(imageService.saveHRoomImages(any())).thenReturn(List.of("/uploads/rooms/room.webp"));

        mockMvc.perform(multipart("/manager/hotels/hotel-1/rooms/add")
                        .file(new MockMultipartFile("imageFiles", "room.webp", "image/webp", new byte[]{1, 2, 3}))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/hotels/hotel-1/rooms"));

        verify(roomService).createRoom(any(Room.class), eq("hotel-1"));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void updateRoom_whenRoomBelongsToHotel_updatesAndRedirects() throws Exception
    {
        when(managerService.isHotelOwnedByManager("hotel-1", "manager-1")).thenReturn(true);
        when(roomService.getRoomById("room-1")).thenReturn(room);

        mockMvc.perform(post("/manager/hotels/hotel-1/rooms/edit/room-1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/hotels/hotel-1/rooms"));

        verify(roomService).updateRoom(eq("room-1"), any(Room.class));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void deleteRoom_whenManagerOwnsHotel_deletesAndRedirects() throws Exception
    {
        when(managerService.isHotelOwnedByManager("hotel-1", "manager-1")).thenReturn(true);
        when(roomService.getRoomById("room-1")).thenReturn(room);

        mockMvc.perform(post("/manager/hotels/hotel-1/rooms/delete/room-1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/hotels/hotel-1/rooms"));

        verify(roomService).deleteRoom("room-1");
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void deleteRoom_whenManagerDoesNotOwnHotel_doesNotDelete() throws Exception
    {
        when(managerService.isHotelOwnedByManager("hotel-1", "manager-1")).thenReturn(false);

        mockMvc.perform(post("/manager/hotels/hotel-1/rooms/delete/room-1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/dashboard"));

        verify(roomService, never()).deleteRoom(anyString());
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void updateRoomImage_appendsImagesAndRedirects() throws Exception
    {
        room.setRoomImages(List.of("/uploads/rooms/old.webp"));
        room.setRoomCoverImage("/uploads/rooms/old.webp");

        when(managerService.isHotelOwnedByManager("hotel-1", "manager-1")).thenReturn(true);
        when(roomService.getRoomById("room-1")).thenReturn(room);
        when(imageService.saveHRoomImages(any())).thenReturn(List.of("/uploads/rooms/new.webp"));

        mockMvc.perform(multipart("/manager/hotels/hotel-1/rooms/edit/image/room-1")
                        .file(new MockMultipartFile("imageFiles", "new.webp", "image/webp", new byte[]{1, 2, 3}))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/hotels/hotel-1/rooms/edit/room-1"));

        verify(roomService).addRoomImage(eq("room-1"), any(Room.class));
    }
}