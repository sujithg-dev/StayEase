
package com.stayease.service;

import com.stayease.dto.HotelSearchRequest;
import com.stayease.model.Hotel;
import com.stayease.model.Room;
import com.stayease.repository.HotelRepository;
import com.stayease.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelServiceTest
{
    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomService roomService;

    @Mock
    private ImageService imageService;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private HotelService hotelService;

    private Hotel hotel;

    @BeforeEach
    void setUp()
    {
        hotel = new Hotel();
        hotel.setHotelId("hotel123");
        hotel.setHotelName("Coastal Pearl Resort");
        hotel.setManagerId("manager123");
        hotel.setHotelStatus(Hotel.HotelStatus.ACTIVE);
        hotel.setHotelRating(4.5);
        hotel.setHotelStreet("Beach Road");
        hotel.setHotelCity("Puducherry");
        hotel.setHotelState("Puducherry");
        hotel.setHotelCountry("India");
        hotel.setHotelAmenities(new ArrayList<>(List.of("WiFi", "Pool")));
        hotel.setHotelImages(new ArrayList<>(List.of("image1.webp", "image2.webp")));
        hotel.setHotelCoverImage("image1.webp");
    }

    private Room room(double price, int availableRooms)
    {
        Room room = new Room();
        room.setRoomId("room1");
        room.setHotelId("hotel123");
        room.setRoomPrice(price);
        room.setRoomCapacity(2);
        room.setAvailableRooms(availableRooms);
        room.setRoomStatus(Room.RoomStatus.AVAILABLE);
        return room;
    }

    private HotelSearchRequest searchRequest()
    {
        return new HotelSearchRequest();
    }

    @Test
    void getAllHotels_returnsAllHotels()
    {
        when(hotelRepository.findAll()).thenReturn(List.of(hotel));

        List<Hotel> result = hotelService.getAllHotels();

        assertEquals(1, result.size());
        assertSame(hotel, result.getFirst());
        verify(hotelRepository).findAll();
    }

    @Test
    void searchHotels_withoutFilters_returnsActiveHotelsWithPrices()
    {
        when(hotelRepository.findByHotelStatus(Hotel.HotelStatus.ACTIVE)).thenReturn(List.of(hotel));
        when(roomRepository.findByHotelId("hotel123")).thenReturn(List.of(room(2500, 3)));
        when(roomService.getStartingPrice("hotel123")).thenReturn(2500.0);
        when(roomService.getHighestrice("hotel123")).thenReturn(4500.0);

        Page<Hotel> result = hotelService.searchHotels(searchRequest(), 0, 10, "recommended");

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(2500.0, result.getContent().getFirst().getStartingPrice());
        assertEquals(4500.0, result.getContent().getFirst().getHighestPrice());
    }

    @Test
    void searchHotels_filtersByDestination()
    {
        Hotel otherHotel = new Hotel();
        otherHotel.setHotelId("hotel456");
        otherHotel.setHotelName("Mountain Stay");
        otherHotel.setHotelRating(4.0);
        otherHotel.setHotelCity("Ooty");
        otherHotel.setHotelStatus(Hotel.HotelStatus.ACTIVE);

        HotelSearchRequest request = searchRequest();
        request.setDestination("Puducherry");

        when(hotelRepository.findByHotelStatus(Hotel.HotelStatus.ACTIVE)).thenReturn(List.of(hotel, otherHotel));
        when(roomRepository.findByHotelId("hotel123")).thenReturn(List.of(room(2500, 2)));
        when(roomService.getStartingPrice("hotel123")).thenReturn(2500.0);
        when(roomService.getHighestrice("hotel123")).thenReturn(4500.0);

        Page<Hotel> result = hotelService.searchHotels(request, 0, 10, "recommended");

        assertEquals(1, result.getTotalElements());
        assertEquals("hotel123", result.getContent().getFirst().getHotelId());
        verify(roomRepository, never()).findByHotelId("hotel456");
    }

    @Test
    void searchHotels_filtersByMinimumRating()
    {
        HotelSearchRequest request = searchRequest();
        request.setMinRating(4.0);

        when(hotelRepository.findByHotelStatus(Hotel.HotelStatus.ACTIVE)).thenReturn(List.of(hotel));
        when(roomRepository.findByHotelId("hotel123")).thenReturn(List.of(room(2500, 2)));
        when(roomService.getStartingPrice("hotel123")).thenReturn(2500.0);
        when(roomService.getHighestrice("hotel123")).thenReturn(4500.0);

        Page<Hotel> result = hotelService.searchHotels(request, 0, 10, "recommended");

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchHotels_filtersByAmenities()
    {
        HotelSearchRequest request = searchRequest();
        request.setAmenities(List.of("WiFi", "Pool"));

        when(hotelRepository.findByHotelStatus(Hotel.HotelStatus.ACTIVE)).thenReturn(List.of(hotel));
        when(roomRepository.findByHotelId("hotel123")).thenReturn(List.of(room(2500, 2)));
        when(roomService.getStartingPrice("hotel123")).thenReturn(2500.0);
        when(roomService.getHighestrice("hotel123")).thenReturn(4500.0);

        Page<Hotel> result = hotelService.searchHotels(request, 0, 10, "recommended");

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchHotels_excludesHotelsWithoutMatchingAmenities()
    {
        HotelSearchRequest request = searchRequest();
        request.setAmenities(List.of("Gym"));

        when(hotelRepository.findByHotelStatus(Hotel.HotelStatus.ACTIVE)).thenReturn(List.of(hotel));

        Page<Hotel> result = hotelService.searchHotels(request, 0, 10, "recommended");

        assertTrue(result.isEmpty());
        verify(roomService, never()).getStartingPrice(anyString());
    }

    @Test
    void searchHotels_filtersByPriceAndGuestCapacity()
    {
        HotelSearchRequest request = searchRequest();
        request.setMinPrice(2000.0);
        request.setMaxPrice(3000.0);
        request.setGuests(2);

        when(hotelRepository.findByHotelStatus(Hotel.HotelStatus.ACTIVE)).thenReturn(List.of(hotel));
        when(roomRepository.findByHotelId("hotel123")).thenReturn(List.of(room(2500, 2)));
        when(roomService.getStartingPrice("hotel123")).thenReturn(2500.0);
        when(roomService.getHighestrice("hotel123")).thenReturn(4500.0);

        Page<Hotel> result = hotelService.searchHotels(request, 0, 10, "recommended");

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchHotels_excludesUnavailableRooms()
    {
        HotelSearchRequest request = searchRequest();
        Room unavailableRoom = room(2500, 3);
        unavailableRoom.setRoomStatus(Room.RoomStatus.UNAVAILABLE);

        when(hotelRepository.findByHotelStatus(Hotel.HotelStatus.ACTIVE)).thenReturn(List.of(hotel));
        when(roomRepository.findByHotelId("hotel123")).thenReturn(List.of(unavailableRoom));

        Page<Hotel> result = hotelService.searchHotels(request, 0, 10, "recommended");

        assertTrue(result.isEmpty());
    }

    @Test
    void searchHotels_excludesRoomsWithNoAvailability()
    {
        when(hotelRepository.findByHotelStatus(Hotel.HotelStatus.ACTIVE)).thenReturn(List.of(hotel));
        when(roomRepository.findByHotelId("hotel123")).thenReturn(List.of(room(2500, 0)));

        Page<Hotel> result = hotelService.searchHotels(searchRequest(), 0, 10, "recommended");

        assertTrue(result.isEmpty());
    }

    @Test
    void searchHotels_sortsByPriceAscending()
    {
        Hotel secondHotel = new Hotel();
        secondHotel.setHotelId("hotel456");
        secondHotel.setHotelName("Budget Hotel");
        secondHotel.setHotelRating(4.0);
        secondHotel.setHotelStatus(Hotel.HotelStatus.ACTIVE);

        when(hotelRepository.findByHotelStatus(Hotel.HotelStatus.ACTIVE)).thenReturn(List.of(hotel, secondHotel));
        when(roomRepository.findByHotelId(anyString())).thenReturn(List.of(room(1000, 2)));
        when(roomService.getStartingPrice("hotel123")).thenReturn(2500.0);
        when(roomService.getHighestrice("hotel123")).thenReturn(4500.0);
        when(roomService.getStartingPrice("hotel456")).thenReturn(1000.0);
        when(roomService.getHighestrice("hotel456")).thenReturn(1500.0);

        Page<Hotel> result = hotelService.searchHotels(searchRequest(), 0, 10, "priceAsc");

        assertEquals("hotel456", result.getContent().getFirst().getHotelId());
        assertEquals("hotel123", result.getContent().get(1).getHotelId());
    }

    @Test
    void searchHotels_sortsByNameAscending()
    {
        Hotel secondHotel = new Hotel();
        secondHotel.setHotelId("hotel456");
        secondHotel.setHotelName("Budget Hotel");
        secondHotel.setHotelRating(4.0);
        secondHotel.setHotelStatus(Hotel.HotelStatus.ACTIVE);

        when(hotelRepository.findByHotelStatus(Hotel.HotelStatus.ACTIVE)).thenReturn(List.of(hotel, secondHotel));
        when(roomRepository.findByHotelId(anyString())).thenReturn(List.of(room(1000, 2)));
        when(roomService.getStartingPrice(anyString())).thenReturn(1000.0);
        when(roomService.getHighestrice(anyString())).thenReturn(2000.0);

        Page<Hotel> result = hotelService.searchHotels(searchRequest(), 0, 10, "nameAsc");

        assertEquals("Budget Hotel", result.getContent().getFirst().getHotelName());
        assertEquals("Coastal Pearl Resort", result.getContent().get(1).getHotelName());
    }

    @Test
    void searchHotels_returnsCorrectPageAndTotalElements()
    {
        Hotel secondHotel = new Hotel();
        secondHotel.setHotelId("hotel456");
        secondHotel.setHotelName("Mountain Stay");
        secondHotel.setHotelRating(4.0);
        secondHotel.setHotelStatus(Hotel.HotelStatus.ACTIVE);

        when(hotelRepository.findByHotelStatus(Hotel.HotelStatus.ACTIVE)).thenReturn(List.of(hotel, secondHotel));
        when(roomRepository.findByHotelId(anyString())).thenReturn(List.of(room(1000, 2)));
        when(roomService.getStartingPrice(anyString())).thenReturn(1000.0);
        when(roomService.getHighestrice(anyString())).thenReturn(2000.0);

        Page<Hotel> result = hotelService.searchHotels(searchRequest(), 0, 1, "recommended");

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getTotalPages());      // 2 elements / page size 1
        assertEquals(1, result.getContent().size());  // first page holds one hotel
        assertEquals(0, result.getNumber());
    }

    @Test
    void searchHotels_whenPageIsOutOfRange_returnsEmptyPage()
    {
        when(hotelRepository.findByHotelStatus(Hotel.HotelStatus.ACTIVE)).thenReturn(List.of(hotel));
        when(roomRepository.findByHotelId("hotel123")).thenReturn(List.of(room(2500, 2)));
        when(roomService.getStartingPrice("hotel123")).thenReturn(2500.0);
        when(roomService.getHighestrice("hotel123")).thenReturn(4500.0);

        Page<Hotel> result = hotelService.searchHotels(searchRequest(), 5, 10, "recommended");

        assertTrue(result.isEmpty());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getHotelById_whenHotelExists_returnsHotel()
    {
        when(hotelRepository.findById("hotel123")).thenReturn(Optional.of(hotel));

        assertSame(hotel, hotelService.getHotelById("hotel123"));
    }

    @Test
    void getHotelById_whenHotelDoesNotExist_throwsException()
    {
        when(hotelRepository.findById("missing")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> hotelService.getHotelById("missing"));

        assertEquals("Hotel not found!", exception.getMessage());
    }

    @Test
    void createHotel_setsManagerAndActiveStatus()
    {
        hotel.setHotelRating(4.0);

        hotelService.createHotel(hotel, "manager456");

        assertEquals("manager456", hotel.getManagerId());
        assertEquals(Hotel.HotelStatus.ACTIVE, hotel.getHotelStatus());
        verify(hotelRepository).save(hotel);
    }

    @Test
    void createHotel_whenRatingIsBelowZero_setsRatingToZero()
    {
        hotel.setHotelRating(-2.0);

        hotelService.createHotel(hotel, "manager123");

        assertEquals(0.0, hotel.getHotelRating());
        verify(hotelRepository).save(hotel);
    }

    @Test
    void createHotel_whenRatingExceedsFive_setsRatingToFive()
    {
        hotel.setHotelRating(8.0);

        hotelService.createHotel(hotel, "manager123");

        assertEquals(5.0, hotel.getHotelRating());
        verify(hotelRepository).save(hotel);
    }

    @Test
    void updateHotel_whenManagerOwnsHotel_updatesFields()
    {
        Hotel updatedHotel = new Hotel();
        updatedHotel.setHotelName("Updated Resort");
        updatedHotel.setHotelStatus(Hotel.HotelStatus.INACTIVE);
        updatedHotel.setHotelDescription("Updated description");
        updatedHotel.setHotelCity("Chennai");
        updatedHotel.setHotelRating(4.8);
        updatedHotel.setHotelAmenities(List.of("WiFi"));
        updatedHotel.setHotelPolicies(List.of("No smoking"));

        when(hotelRepository.findById("hotel123")).thenReturn(Optional.of(hotel));

        hotelService.updateHotel("hotel123", updatedHotel, "manager123");

        assertEquals("Updated Resort", hotel.getHotelName());
        assertEquals(Hotel.HotelStatus.INACTIVE, hotel.getHotelStatus());
        assertEquals("Updated description", hotel.getHotelDescription());
        assertEquals("Chennai", hotel.getHotelCity());
        assertEquals(4.8, hotel.getHotelRating());
        assertEquals(List.of("WiFi"), hotel.getHotelAmenities());
        assertEquals(List.of("No smoking"), hotel.getHotelPolicies());

        verify(hotelRepository).save(hotel);
    }

    @Test
    void updateHotel_whenManagerDoesNotOwnHotel_throwsException()
    {
        when(hotelRepository.findById("hotel123")).thenReturn(Optional.of(hotel));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> hotelService.updateHotel("hotel123", new Hotel(), "otherManager"));

        assertEquals("You are not authorized to modify this hotel", exception.getMessage());
        verify(hotelRepository, never()).save(any(Hotel.class));
    }

    @Test
    void deleteHotel_whenManagerOwnsHotel_deletesHotel()
    {
        when(hotelRepository.findById("hotel123")).thenReturn(Optional.of(hotel));

        hotelService.deleteHotel("hotel123", "manager123");

        verify(hotelRepository).delete(hotel);
    }

    @Test
    void deleteHotel_whenManagerDoesNotOwnHotel_throwsException()
    {
        when(hotelRepository.findById("hotel123")).thenReturn(Optional.of(hotel));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> hotelService.deleteHotel("hotel123", "otherManager"));

        assertEquals("You are not authorized to delete this hotel", exception.getMessage());
        verify(hotelRepository, never()).delete(any(Hotel.class));
    }

    @Test
    void addHotelImage_whenManagerOwnsHotel_updatesImages()
    {
        Hotel updatedHotel = new Hotel();
        updatedHotel.setHotelImages(new ArrayList<>(List.of("image3.webp")));
        updatedHotel.setHotelCoverImage("image3.webp");

        when(hotelRepository.findById("hotel123")).thenReturn(Optional.of(hotel));

        hotelService.addHotelImage("hotel123", updatedHotel, "manager123");

        assertEquals(List.of("image3.webp"), hotel.getHotelImages());
        assertEquals("image3.webp", hotel.getHotelCoverImage());
        verify(hotelRepository).save(hotel);
    }

    @Test
    void addHotelImage_whenManagerDoesNotOwnHotel_throwsException()
    {
        when(hotelRepository.findById("hotel123")).thenReturn(Optional.of(hotel));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> hotelService.addHotelImage("hotel123", new Hotel(), "otherManager"));

        assertEquals("You are not authorized to modify this hotel", exception.getMessage());
        verify(hotelRepository, never()).save(any(Hotel.class));
    }

    @Test
    void setHotelCoverImage_updatesMatchingHotel()
    {
        hotelService.setHotelCoverImage("hotel123", "image2.webp", "manager123");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Hotel.class));
    }

    @Test
    void deleteHotelImage_whenImageIsNotCover_removesImageAndFile()
    {
        when(hotelRepository.findById("hotel123")).thenReturn(Optional.of(hotel));

        hotelService.deleteHotelImage("hotel123", "image2.webp", "manager123");

        assertEquals(List.of("image1.webp"), hotel.getHotelImages());
        assertEquals("image1.webp", hotel.getHotelCoverImage());
        verify(hotelRepository).save(hotel);
        verify(imageService).deleteHotelImageFile("image2.webp");
    }

    @Test
    void deleteHotelImage_whenCoverImageIsDeleted_selectsNextImage()
    {
        when(hotelRepository.findById("hotel123")).thenReturn(Optional.of(hotel));

        hotelService.deleteHotelImage("hotel123", "image1.webp", "manager123");

        assertEquals(List.of("image2.webp"), hotel.getHotelImages());
        assertEquals("image2.webp", hotel.getHotelCoverImage());
        verify(hotelRepository).save(hotel);
        verify(imageService).deleteHotelImageFile("image1.webp");
    }

    @Test
    void deleteHotelImage_whenLastImageIsDeleted_clearsCover()
    {
        hotel.setHotelImages(new ArrayList<>(List.of("image1.webp")));
        hotel.setHotelCoverImage("image1.webp");

        when(hotelRepository.findById("hotel123")).thenReturn(Optional.of(hotel));

        hotelService.deleteHotelImage("hotel123", "image1.webp", "manager123");

        assertTrue(hotel.getHotelImages().isEmpty());
        assertNull(hotel.getHotelCoverImage());
        verify(hotelRepository).save(hotel);
        verify(imageService).deleteHotelImageFile("image1.webp");
    }

    @Test
    void deleteHotelImage_whenHotelDoesNotExist_doesNothing()
    {
        when(hotelRepository.findById("missing")).thenReturn(Optional.empty());

        hotelService.deleteHotelImage("missing", "image1.webp", "manager123");

        verify(hotelRepository, never()).save(any(Hotel.class));
        verifyNoInteractions(imageService);
    }

    @Test
    void deleteHotelImage_whenManagerDoesNotOwnHotel_doesNothing()
    {
        when(hotelRepository.findById("hotel123")).thenReturn(Optional.of(hotel));

        hotelService.deleteHotelImage("hotel123", "image1.webp", "otherManager");

        assertEquals(2, hotel.getHotelImages().size());
        verify(hotelRepository, never()).save(any(Hotel.class));
        verifyNoInteractions(imageService);
    }

    @Test
    void deleteHotelImage_whenImageDoesNotExist_doesNothing()
    {
        when(hotelRepository.findById("hotel123")).thenReturn(Optional.of(hotel));

        hotelService.deleteHotelImage("hotel123", "missing.webp", "manager123");

        assertEquals(2, hotel.getHotelImages().size());
        verify(hotelRepository, never()).save(any(Hotel.class));
        verifyNoInteractions(imageService);
    }
}
