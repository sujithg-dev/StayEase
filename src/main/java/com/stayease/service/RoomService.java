package com.stayease.service;

import com.stayease.model.Room;
import com.stayease.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RoomService
{
    private final RoomRepository roomRepository;
    private final ImageService imageService;
    private final MongoTemplate mongoTemplate;

    public List<Room> getHotelRooms(String hotelId)
    {
        return roomRepository.findByHotelId(hotelId);
    }

    public List<Room> getAvailableHotelRooms(String hotelId)
    {
        return roomRepository.findByHotelIdAndRoomStatusAndAvailableRoomsGreaterThan(hotelId, Room.RoomStatus.AVAILABLE, 0);
    }

    public Room getRoomById(String id)
    {
        return roomRepository.findById(id).orElseThrow(() -> new RuntimeException("Room not found!"));
    }

    public void createRoom(Room room, String hotelId)
    {
        room.setHotelId(hotelId);

        if (room.getAvailableRooms() == 0)
        {
            room.setAvailableRooms(room.getTotalRooms());
        }

        room.setRoomStatus(Room.RoomStatus.AVAILABLE);

        roomRepository.save(room);
    }

    public void updateRoom(String id, Room updatedRoom)
    {
        Room existingRoom = getRoomById(id);

        existingRoom.setRoomName(updatedRoom.getRoomName());
        existingRoom.setRoomType(updatedRoom.getRoomType());
        existingRoom.setRoomDescription(updatedRoom.getRoomDescription());
        existingRoom.setRoomPrice(updatedRoom.getRoomPrice());
        existingRoom.setRoomCapacity(updatedRoom.getRoomCapacity());
        existingRoom.setTotalRooms(updatedRoom.getTotalRooms());
        existingRoom.setAvailableRooms(updatedRoom.getAvailableRooms());
        existingRoom.setRoomStatus(updatedRoom.getRoomStatus());

        roomRepository.save(existingRoom);
    }

    public void deleteRoom(String id)
    {
        Room room = getRoomById(id);

        roomRepository.delete(room);
    }

    public void addRoomImage(String id, Room updatedRoom)
    {
        Room existingRoom = getRoomById(id);

        if (existingRoom == null)
        {
            throw new RuntimeException("Room not found");
        }

        existingRoom.setRoomImages(updatedRoom.getRoomImages());
        existingRoom.setRoomCoverImage(updatedRoom.getRoomCoverImage());

        roomRepository.save(existingRoom);
    }

    public void setRoomCoverImage(String roomId, String image)
    {
        Query query = new Query(
                Criteria.where("_id").is(roomId)
                        .and("roomImages").in(image)
        );

        Update update = new Update();
        update.set("roomCoverImage", image);

        mongoTemplate.updateFirst(query, update, Room.class);
    }

    public void deleteRoomImage(String roomId, String image)
    {
        Room room = roomRepository.findById(roomId).orElse(null);

        if (room.getRoomImages() == null || !room.getRoomImages().contains(image))
        {
            return;
        }

        room.getRoomImages().remove(image);

        if (image.equals(room.getRoomCoverImage()))
        {
            if (room.getRoomImages().isEmpty())
            {
                room.setRoomCoverImage(null);
            }
            else
            {
                room.setRoomCoverImage(room.getRoomImages().getFirst());
            }
        }

        roomRepository.save(room);
        imageService.deleteRoomImageFile(image);
    }

    public Double getStartingPrice(String hotelId)
    {
        return roomRepository.findByHotelId(hotelId).stream()
                .map(Room::getRoomPrice)
                .filter(Objects::nonNull)
                .min(Double::compareTo)
                .orElse(null);
    }
}