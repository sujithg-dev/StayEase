package com.stayease.service;

import com.stayease.dto.BookingRequest;
import com.stayease.model.Booking;
import com.stayease.model.Hotel;
import com.stayease.model.Room;
import com.stayease.model.User;
import com.stayease.repository.BookingRepository;
import com.stayease.repository.HotelRepository;
import com.stayease.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest
{
    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private BookingService bookingService;

    private Hotel hotel;
    private Room room;
    private User customer;
    private BookingRequest request;

    @BeforeEach
    void setUp()
    {
        hotel = new Hotel();
        hotel.setHotelId("hotel-1");
        hotel.setHotelName("StayEase Hotel");
        hotel.setHotelStatus(Hotel.HotelStatus.ACTIVE);

        room = new Room();
        room.setRoomId("room-1");
        room.setHotelId("hotel-1");
        room.setRoomStatus(Room.RoomStatus.AVAILABLE);
        room.setRoomCapacity(4);
        room.setRoomPrice(2000.0);
        room.setTotalRooms(5);
        room.setAvailableRooms(5);

        customer = new User();
        customer.setUserId("customer-1");
        customer.setUserName("Test Customer");
        customer.setUserEmail("customer@example.com");

        request = new BookingRequest();
        request.setCheckInDate(LocalDate.now().plusDays(2));
        request.setCheckOutDate(LocalDate.now().plusDays(4));
        request.setTotalGuests(2);
        request.setTotalRooms(1);
    }

    private Booking createBooking(Booking.BookingStatus status, LocalDate checkIn, LocalDate checkOut)
    {
        Booking booking = new Booking();
        booking.setBookingId("booking-1");
        booking.setBookingNumber("SE-12345678");
        booking.setCustomerId("customer-1");
        booking.setHotelId("hotel-1");
        booking.setRoomId("room-1");
        booking.setTotalRooms(1);
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setBookingStatus(status);
        booking.setPaymentStatus(Booking.PaymentStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());
        return booking;
    }

    @Test
    void getHotelBookings_returnsBookings()
    {
        List<Booking> bookings = List.of(createBooking(
                Booking.BookingStatus.CONFIRMED,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3)));

        when(bookingRepository.findByHotelIdOrderByCreatedAtDesc("hotel-1")).thenReturn(bookings);

        assertEquals(bookings, bookingService.getHotelBookings("hotel-1"));
    }

    @Test
    void getCustomerBookings_returnsBookings()
    {
        List<Booking> bookings = List.of(createBooking(
                Booking.BookingStatus.CONFIRMED,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3)));

        when(bookingRepository.findByCustomerIdOrderByCreatedAtDesc("customer-1")).thenReturn(bookings);

        assertEquals(bookings, bookingService.getCustomerBookings("customer-1"));
    }

    @Test
    void getBookingById_whenFound_returnsBooking()
    {
        Booking booking = createBooking(Booking.BookingStatus.CONFIRMED, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));

        assertSame(booking, bookingService.getBookingById("booking-1"));
    }

    @Test
    void getBookingById_whenMissing_throwsException()
    {
        when(bookingRepository.findById("missing")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> bookingService.getBookingById("missing"));

        assertEquals("Booking not found", exception.getMessage());
    }

    @Test
    void getCustomerBooking_whenOwned_returnsBooking()
    {
        Booking booking = createBooking(Booking.BookingStatus.CONFIRMED, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));

        assertSame(booking, bookingService.getCustomerBooking("booking-1", "customer-1"));
    }

    @Test
    void getCustomerBooking_whenNotOwned_throwsException()
    {
        Booking booking = createBooking(Booking.BookingStatus.CONFIRMED, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> bookingService.getCustomerBooking("booking-1", "another-customer"));

        assertEquals("You are not allowed to view this booking", exception.getMessage());
    }

    @Test
    void getHotelBooking_whenOwned_returnsBooking()
    {
        Booking booking = createBooking(Booking.BookingStatus.CONFIRMED, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));

        assertSame(booking, bookingService.getHotelBooking("booking-1", "hotel-1"));
    }

    @Test
    void getHotelBooking_whenNotOwned_throwsException()
    {
        Booking booking = createBooking(Booking.BookingStatus.CONFIRMED, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> bookingService.getHotelBooking("booking-1", "another-hotel"));

        assertEquals("This booking does not belong to your hotel", exception.getMessage());
    }

    @Test
    void createBooking_whenValid_savesBookingAndReducesAvailability()
    {
        when(hotelRepository.findById("hotel-1")).thenReturn(Optional.of(hotel));
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(bookingRepository.findByRoomIdAndBookingStatusAndCheckInDateLessThanAndCheckOutDateGreaterThan(
                eq("room-1"), anyList(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(bookingRepository.existsByBookingNumber(anyString())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking saved = bookingService.createBooking(request, "hotel-1", "room-1", customer);

        assertNotNull(saved.getBookingNumber());
        assertTrue(saved.getBookingNumber().startsWith("SE-"));
        assertEquals("customer-1", saved.getCustomerId());
        assertEquals("StayEase Hotel", saved.getHotelName());
        assertEquals("room-1", saved.getRoomId());
        assertEquals(Booking.BookingStatus.PENDING_PAYMENT, saved.getBookingStatus());
        assertEquals(Booking.PaymentStatus.PENDING, saved.getPaymentStatus());
        assertEquals(new BigDecimal("2000.0"), saved.getRoomPrice());
        assertEquals(new BigDecimal("4000.00"), saved.getTotalAmount());
        verify(bookingRepository).save(any(Booking.class));
        verify(roomRepository).save(room);
        assertEquals(4, room.getAvailableRooms());
    }

    @Test
    void createBooking_whenHotelMissing_throwsException()
    {
        when(hotelRepository.findById("hotel-1")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> bookingService.createBooking(request, "hotel-1", "room-1", customer));

        assertEquals("Hotel not found!", exception.getMessage());
        verifyNoInteractions(roomRepository);
    }

    @Test
    void createBooking_whenHotelInactive_throwsException()
    {
        hotel.setHotelStatus(Hotel.HotelStatus.INACTIVE);
        when(hotelRepository.findById("hotel-1")).thenReturn(Optional.of(hotel));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> bookingService.createBooking(request, "hotel-1", "room-1", customer));

        assertEquals("This hotel is currently unavailable", exception.getMessage());
    }

    @Test
    void createBooking_whenRoomMissing_throwsException()
    {
        when(hotelRepository.findById("hotel-1")).thenReturn(Optional.of(hotel));
        when(roomRepository.findById("room-1")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> bookingService.createBooking(request, "hotel-1", "room-1", customer));

        assertEquals("Room not found!", exception.getMessage());
    }

    @Test
    void createBooking_whenRoomBelongsToAnotherHotel_throwsException()
    {
        room.setHotelId("another-hotel");
        when(hotelRepository.findById("hotel-1")).thenReturn(Optional.of(hotel));
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> bookingService.createBooking(request, "hotel-1", "room-1", customer));

        assertEquals("Room does not belong to this hotel", exception.getMessage());
    }

    @Test
    void createBooking_whenRoomUnavailable_throwsException()
    {
        room.setRoomStatus(Room.RoomStatus.UNAVAILABLE);
        when(hotelRepository.findById("hotel-1")).thenReturn(Optional.of(hotel));
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> bookingService.createBooking(request, "hotel-1", "room-1", customer));

        assertEquals("This room is currently unavailable", exception.getMessage());
    }

    @Test
    void createBooking_whenDatesMissing_throwsException()
    {
        request.setCheckInDate(null);
        when(hotelRepository.findById("hotel-1")).thenReturn(Optional.of(hotel));
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> bookingService.createBooking(request, "hotel-1", "room-1", customer));

        assertEquals("Check-in and check-out dates are required", exception.getMessage());
    }

    @Test
    void createBooking_whenCheckInInPast_throwsException()
    {
        request.setCheckInDate(LocalDate.now().minusDays(1));
        request.setCheckOutDate(LocalDate.now().plusDays(1));
        when(hotelRepository.findById("hotel-1")).thenReturn(Optional.of(hotel));
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> bookingService.createBooking(request, "hotel-1", "room-1", customer));

        assertEquals("Check-in date cannot be in the past", exception.getMessage());
    }

    @Test
    void createBooking_whenCheckOutNotAfterCheckIn_throwsException()
    {
        request.setCheckOutDate(request.getCheckInDate());

        when(hotelRepository.findById("hotel-1")).thenReturn(Optional.of(hotel));
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> bookingService.createBooking(request, "hotel-1", "room-1", customer));

        assertEquals("Check-out date must be after check-in date", exception.getMessage());
    }

    @Test
    void createBooking_whenGuestsNotPositive_throwsException()
    {
        request.setTotalGuests(0);
        when(hotelRepository.findById("hotel-1")).thenReturn(Optional.of(hotel));
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> bookingService.createBooking(request, "hotel-1", "room-1", customer));

        assertEquals("At least one guest is required", exception.getMessage());
    }

    @Test
    void createBooking_whenGuestsExceedCapacity_throwsException()
    {
        request.setTotalGuests(5);
        when(hotelRepository.findById("hotel-1")).thenReturn(Optional.of(hotel));
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> bookingService.createBooking(request, "hotel-1", "room-1", customer));

        assertEquals("This room can accommodate maximum 4 guests", exception.getMessage());
    }

    @Test
    void createBooking_whenRoomCountNotPositive_throwsException()
    {
        request.setTotalRooms(0);
        when(hotelRepository.findById("hotel-1")).thenReturn(Optional.of(hotel));
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> bookingService.createBooking(request, "hotel-1", "room-1", customer));

        assertEquals("At least one room must be booked", exception.getMessage());
    }

    @Test
    void createBooking_whenOverlappingBookingsExceedAvailability_throwsException()
    {
        Booking existing = createBooking(Booking.BookingStatus.CONFIRMED, LocalDate.now().plusDays(1), LocalDate.now().plusDays(5));
        existing.setTotalRooms(5);

        when(hotelRepository.findById("hotel-1")).thenReturn(Optional.of(hotel));
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(bookingRepository.findByRoomIdAndBookingStatusAndCheckInDateLessThanAndCheckOutDateGreaterThan(
                eq("room-1"), anyList(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(existing));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> bookingService.createBooking(request, "hotel-1", "room-1", customer));

        assertEquals("Only 0 room(s) are available for the selected dates", exception.getMessage());
    }

    @Test
    void createBooking_whenBookingNumberAlreadyExists_generatesAnotherNumber()
    {
        when(hotelRepository.findById("hotel-1")).thenReturn(Optional.of(hotel));
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(bookingRepository.findByRoomIdAndBookingStatusAndCheckInDateLessThanAndCheckOutDateGreaterThan(
                eq("room-1"), anyList(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(bookingRepository.existsByBookingNumber(anyString())).thenReturn(true, false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking saved = bookingService.createBooking(request, "hotel-1", "room-1", customer);

        assertTrue(saved.getBookingNumber().matches("SE-[A-F0-9]{8}"));
        verify(bookingRepository, times(2)).existsByBookingNumber(anyString());
    }

    @Test
    void cancelBooking_whenValid_cancelsAndRestoresRooms()
    {
        Booking booking = createBooking(Booking.BookingStatus.CONFIRMED, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4));
        room.setAvailableRooms(3);

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

        bookingService.cancelBooking("booking-1", "customer-1");

        assertEquals(Booking.BookingStatus.CANCELLED, booking.getBookingStatus());
        assertEquals(4, room.getAvailableRooms());
        verify(emailService).sendBookingCancellation(booking);
    }

    @Test
    void cancelBooking_whenWrongCustomer_throwsException()
    {
        Booking booking = createBooking(Booking.BookingStatus.CONFIRMED, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4));
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> bookingService.cancelBooking("booking-1", "another-customer"));

        assertEquals("You are not allowed to cancel this booking", exception.getMessage());
    }

    @Test
    void cancelBooking_whenNotConfirmed_throwsException()
    {
        Booking booking = createBooking(Booking.BookingStatus.PENDING_PAYMENT, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4));
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> bookingService.cancelBooking("booking-1", "customer-1"));

        assertEquals("This booking cannot be cancelled", exception.getMessage());
    }

    @Test
    void cancelBooking_whenCheckInToday_throwsException()
    {
        Booking booking = createBooking(Booking.BookingStatus.CONFIRMED, LocalDate.now(), LocalDate.now().plusDays(2));
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> bookingService.cancelBooking("booking-1", "customer-1"));

        assertEquals("Booking cannot be cancelled after check-in", exception.getMessage());
    }

    @Test
    void cancelBookingByManager_whenValid_cancelsAndRestoresRooms()
    {
        Booking booking = createBooking(Booking.BookingStatus.CONFIRMED, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4));
        room.setAvailableRooms(3);

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

        bookingService.cancelBookingByManager("booking-1", "hotel-1");

        assertEquals(Booking.BookingStatus.CANCELLED, booking.getBookingStatus());
        assertEquals(4, room.getAvailableRooms());
        verify(emailService, never()).sendBookingCancellation(any());
    }

    @Test
    void cancelBookingByManager_whenWrongHotel_throwsException()
    {
        Booking booking = createBooking(Booking.BookingStatus.CONFIRMED, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4));

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> bookingService.cancelBookingByManager("booking-1", "another-hotel"));

        assertEquals("This booking does not belong to your hotel", exception.getMessage());
    }

    @Test
    void updateCompletedBookings_marksPastConfirmedBookingsCompleted()
    {
        Booking booking = createBooking(Booking.BookingStatus.CONFIRMED, LocalDate.now().minusDays(3), LocalDate.now());

        when(bookingRepository.findAll()).thenReturn(List.of(booking));

        bookingService.updateCompletedBookings();

        assertEquals(Booking.BookingStatus.COMPLETED, booking.getBookingStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void updateCompletedBookings_doesNotChangeFutureBookings()
    {
        Booking booking = createBooking(Booking.BookingStatus.CONFIRMED, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        when(bookingRepository.findAll()).thenReturn(List.of(booking));

        bookingService.updateCompletedBookings();

        assertEquals(Booking.BookingStatus.CONFIRMED, booking.getBookingStatus());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void cancelExpiredPendingPayments_cancelsExpiredBookingsAndRestoresRooms()
    {
        Booking booking = createBooking(Booking.BookingStatus.PENDING_PAYMENT, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4));
        booking.setCreatedAt(LocalDateTime.now().minusMinutes(61));
        room.setAvailableRooms(3);

        when(bookingRepository.findAll()).thenReturn(List.of(booking));
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

        bookingService.cancelExpiredPendingPayments();

        assertEquals(Booking.BookingStatus.CANCELLED, booking.getBookingStatus());
        assertEquals(Booking.PaymentStatus.FAILED, booking.getPaymentStatus());
        assertEquals(4, room.getAvailableRooms());
        verify(bookingRepository).save(booking);
    }

    @Test
    void cancelExpiredPendingPayments_doesNotCancelRecentBookings()
    {
        Booking booking = createBooking(Booking.BookingStatus.PENDING_PAYMENT, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4));
        booking.setCreatedAt(LocalDateTime.now().minusMinutes(30));

        when(bookingRepository.findAll()).thenReturn(List.of(booking));

        bookingService.cancelExpiredPendingPayments();

        assertEquals(Booking.BookingStatus.PENDING_PAYMENT, booking.getBookingStatus());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void reduceAvailableRooms_whenEnoughRooms_reducesAvailability()
    {
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

        bookingService.reduceAvailableRooms("room-1", 2);

        assertEquals(3, room.getAvailableRooms());
        verify(roomRepository).save(room);
    }

    @Test
    void reduceAvailableRooms_whenInsufficientRooms_throwsException()
    {
        room.setAvailableRooms(1);
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> bookingService.reduceAvailableRooms("room-1", 2));

        assertEquals("Not enough rooms available", exception.getMessage());
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void restoreAvailableRooms_doesNotExceedTotalRooms()
    {
        room.setAvailableRooms(4);
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

        bookingService.restoreAvailableRooms("room-1", 3);

        assertEquals(5, room.getAvailableRooms());
        verify(roomRepository).save(room);
    }
}