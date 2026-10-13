package com.stayease.service;

import com.stayease.model.Room;
import com.stayease.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest
{
    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private RoomService roomService;

    private Room room;

    @BeforeEach
    void setUp()
    {
        room = new Room();
        room.setRoomId("room-1");
        room.setHotelId("hotel-1");
        room.setRoomName("Deluxe Room");
        room.setRoomType("DELUXE");
        room.setRoomDescription("A comfortable deluxe room");
        room.setRoomPrice(2500.0);
        room.setRoomCapacity(3);
        room.setTotalRooms(5);
        room.setAvailableRooms(4);
        room.setRoomStatus(Room.RoomStatus.AVAILABLE);
        room.setRoomImages(new ArrayList<>(List.of("image1.jpg", "image2.jpg")));
        room.setRoomCoverImage("image1.jpg");
    }

    @Test
    void getHotelRooms_returnsRooms()
    {
        List<Room> rooms = List.of(room);
        when(roomRepository.findByHotelId("hotel-1")).thenReturn(rooms);

        assertEquals(rooms, roomService.getHotelRooms("hotel-1"));
    }

    @Test
    void getAvailableHotelRooms_returnsAvailableRooms()
    {
        List<Room> rooms = List.of(room);
        when(roomRepository.findByHotelIdAndRoomStatusAndAvailableRoomsGreaterThan("hotel-1", Room.RoomStatus.AVAILABLE, 0)).thenReturn(rooms);

        assertEquals(rooms, roomService.getAvailableHotelRooms("hotel-1"));
        verify(roomRepository).findByHotelIdAndRoomStatusAndAvailableRoomsGreaterThan("hotel-1", Room.RoomStatus.AVAILABLE, 0);
    }

    @Test
    void getRoomById_whenFound_returnsRoom()
    {
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

        assertSame(room, roomService.getRoomById("room-1"));
    }

    @Test
    void getRoomById_whenMissing_throwsException()
    {
        when(roomRepository.findById("missing")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> roomService.getRoomById("missing"));

        assertEquals("Room not found!", exception.getMessage());
    }

    @Test
    void createRoom_setsHotelAndDefaultsAvailabilityAndStatus()
    {
        room.setAvailableRooms(0);
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        roomService.createRoom(room, "hotel-2");

        assertEquals("hotel-2", room.getHotelId());
        assertEquals(5, room.getAvailableRooms());
        assertEquals(Room.RoomStatus.AVAILABLE, room.getRoomStatus());
        verify(roomRepository).save(room);
    }

    @Test
    void createRoom_preservesNonZeroAvailability()
    {
        room.setAvailableRooms(2);

        roomService.createRoom(room, "hotel-1");

        assertEquals(2, room.getAvailableRooms());
        assertEquals(Room.RoomStatus.AVAILABLE, room.getRoomStatus());
        verify(roomRepository).save(room);
    }

    @Test
    void updateRoom_updatesFieldsAndSaves()
    {
        Room updatedRoom = new Room();
        updatedRoom.setRoomName("Premium Room");
        updatedRoom.setRoomType("PREMIUM");
        updatedRoom.setRoomDescription("Updated description");
        updatedRoom.setRoomPrice(3500.0);
        updatedRoom.setRoomCapacity(4);
        updatedRoom.setTotalRooms(8);
        updatedRoom.setAvailableRooms(6);
        updatedRoom.setRoomStatus(Room.RoomStatus.UNAVAILABLE);

        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

        roomService.updateRoom("room-1", updatedRoom);

        assertEquals("Premium Room", room.getRoomName());
        assertEquals("PREMIUM", room.getRoomType());
        assertEquals("Updated description", room.getRoomDescription());
        assertEquals(3500.0, room.getRoomPrice());
        assertEquals(4, room.getRoomCapacity());
        assertEquals(8, room.getTotalRooms());
        assertEquals(6, room.getAvailableRooms());
        assertEquals(Room.RoomStatus.UNAVAILABLE, room.getRoomStatus());
        verify(roomRepository).save(room);
    }

    @Test
    void updateRoom_whenMissing_throwsException()
    {
        when(roomRepository.findById("missing")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> roomService.updateRoom("missing", new Room()));

        assertEquals("Room not found!", exception.getMessage());
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void deleteRoom_deletesExistingRoom()
    {
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

        roomService.deleteRoom("room-1");

        verify(roomRepository).delete(room);
    }

    @Test
    void deleteRoom_whenMissing_throwsException()
    {
        when(roomRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> roomService.deleteRoom("missing"));

        verify(roomRepository, never()).delete(any(Room.class));
    }

    @Test
    void addRoomImage_updatesImagesAndCover()
    {
        Room updatedRoom = new Room();
        updatedRoom.setRoomImages(new ArrayList<>(List.of("new1.jpg", "new2.jpg")));
        updatedRoom.setRoomCoverImage("new1.jpg");

        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

        roomService.addRoomImage("room-1", updatedRoom);

        assertEquals(List.of("new1.jpg", "new2.jpg"), room.getRoomImages());
        assertEquals("new1.jpg", room.getRoomCoverImage());
        verify(roomRepository).save(room);
    }

    @Test
    void addRoomImage_whenRoomMissing_throwsException()
    {
        when(roomRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> roomService.addRoomImage("missing", new Room()));
    }

    @Test
    void setRoomCoverImage_updatesMongoTemplate()
    {
        roomService.setRoomCoverImage("room-1", "image2.jpg");

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

        verify(mongoTemplate).updateFirst(queryCaptor.capture(), updateCaptor.capture(), eq(Room.class));

        assertNotNull(queryCaptor.getValue());
        assertNotNull(updateCaptor.getValue());
    }

    @Test
    void deleteRoomImage_removesImageAndDeletesFile()
    {
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

        roomService.deleteRoomImage("room-1", "image2.jpg");

        assertEquals(List.of("image1.jpg"), room.getRoomImages());
        assertEquals("image1.jpg", room.getRoomCoverImage());
        verify(roomRepository).save(room);
        verify(imageService).deleteRoomImageFile("image2.jpg");
    }

    @Test
    void deleteRoomImage_whenDeletingCover_selectsNextImage()
    {
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

        roomService.deleteRoomImage("room-1", "image1.jpg");

        assertEquals(List.of("image2.jpg"), room.getRoomImages());
        assertEquals("image2.jpg", room.getRoomCoverImage());
        verify(imageService).deleteRoomImageFile("image1.jpg");
    }

    @Test
    void deleteRoomImage_whenDeletingLastImage_clearsCover()
    {
        room.setRoomImages(new ArrayList<>(List.of("image1.jpg")));
        room.setRoomCoverImage("image1.jpg");
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

        roomService.deleteRoomImage("room-1", "image1.jpg");

        assertTrue(room.getRoomImages().isEmpty());
        assertNull(room.getRoomCoverImage());
        verify(roomRepository).save(room);
        verify(imageService).deleteRoomImageFile("image1.jpg");
    }

    @Test
    void deleteRoomImage_whenImageNotFound_doesNothing()
    {
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

        roomService.deleteRoomImage("room-1", "missing.jpg");

        verify(roomRepository, never()).save(any(Room.class));
        verify(imageService, never()).deleteRoomImageFile(anyString());
    }

    @Test
    void deleteRoomImage_whenRoomMissing_currentImplementationThrowsNullPointerException()
    {
        when(roomRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(NullPointerException.class, () -> roomService.deleteRoomImage("missing", "image1.jpg"));
    }

    @Test
    void getStartingPrice_returnsMinimumNonNullPrice()
    {
        Room expensive = new Room();
        expensive.setRoomPrice(4000.0);

        Room noPrice = new Room();
        noPrice.setRoomPrice(null);

        when(roomRepository.findByHotelId("hotel-1")).thenReturn(List.of(room, expensive, noPrice));

        assertEquals(2500.0, roomService.getStartingPrice("hotel-1"));
    }

    @Test
    void getStartingPrice_whenNoRooms_returnsNull()
    {
        when(roomRepository.findByHotelId("hotel-1")).thenReturn(List.of());

        assertNull(roomService.getStartingPrice("hotel-1"));
    }

    @Test
    void getHighestrice_returnsMaximumNonNullPrice()
    {
        Room expensive = new Room();
        expensive.setRoomPrice(4000.0);

        Room noPrice = new Room();
        noPrice.setRoomPrice(null);

        when(roomRepository.findByHotelId("hotel-1")).thenReturn(List.of(room, expensive, noPrice));

        assertEquals(4000.0, roomService.getHighestrice("hotel-1"));
    }

    @Test
    void getHighestrice_whenNoRooms_returnsNull()
    {
        when(roomRepository.findByHotelId("hotel-1")).thenReturn(List.of());

        assertNull(roomService.getHighestrice("hotel-1"));
    }
}