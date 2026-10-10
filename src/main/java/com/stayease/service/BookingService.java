package com.stayease.service;

import com.stayease.dto.BookingRequest;
import com.stayease.model.Booking;
import com.stayease.model.Hotel;
import com.stayease.model.Room;
import com.stayease.model.User;
import com.stayease.repository.BookingRepository;
import com.stayease.repository.HotelRepository;
import com.stayease.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService
{
    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final EmailService emailService;

    public List<Booking> getHotelBookings(String hotelId)
    {
        return bookingRepository.findByHotelIdOrderByCreatedAtDesc(hotelId);
    }

    public List<Booking> getCustomerBookings(String customerId)
    {
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public Booking getBookingById(String bookingId)
    {
        return bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));
    }

    public Booking getCustomerBooking(String bookingId, String customerId)
    {
        Booking booking = getBookingById(bookingId);

        if (!booking.getCustomerId().equals(customerId))
        {
            throw new RuntimeException("You are not allowed to view this booking");
        }

        return booking;
    }

    public Booking getHotelBooking(String bookingId, String hotelId)
    {
        Booking booking = getBookingById(bookingId);

        if (!hotelId.equals(booking.getHotelId()))
        {
            throw new RuntimeException("This booking does not belong to your hotel");
        }

        return booking;
    }

    public Booking createBooking(BookingRequest request, String hotelId, String roomId, User customer)
    {
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new RuntimeException("Hotel not found!"));

        if (hotel.getHotelStatus() != Hotel.HotelStatus.ACTIVE)
        {
            throw new RuntimeException("This hotel is currently unavailable");
        }

        Room room = roomRepository.findById(roomId).orElseThrow(() -> new RuntimeException("Room not found!"));

        if (!hotelId.equals(room.getHotelId()))
        {
            throw new RuntimeException("Room does not belong to this hotel");
        }

        if (room.getRoomStatus() != Room.RoomStatus.AVAILABLE)
        {
            throw new RuntimeException("This room is currently unavailable");
        }

        LocalDate checkIn = request.getCheckInDate();
        LocalDate checkOut = request.getCheckOutDate();

        if (checkIn == null || checkOut == null)
        {
            throw new RuntimeException("Check-in and check-out dates are required");
        }

        if (checkIn.isBefore(LocalDate.now()))
        {
            throw new RuntimeException("Check-in date cannot be in the past");
        }

        if (!checkIn.isBefore(checkOut))
        {
            throw new RuntimeException("Check-out date must be after check-in date");
        }

        if (request.getTotalGuests() <= 0)
        {
            throw new RuntimeException("At least one guest is required");
        }

        if (request.getTotalGuests() > room.getRoomCapacity())
        {
            throw new RuntimeException("This room can accommodate maximum " + room.getRoomCapacity() + " guests");
        }

        if (request.getTotalRooms() <= 0)
        {
            throw new RuntimeException("At least one room must be booked");
        }

        List<Booking.BookingStatus> activeStatuses = List.of(
                Booking.BookingStatus.CONFIRMED,
                Booking.BookingStatus.COMPLETED
        );

        List<Booking> overlappingBookings =
                bookingRepository.findByRoomIdAndBookingStatusAndCheckInDateLessThanAndCheckOutDateGreaterThan(
                        roomId, activeStatuses, checkOut, checkIn
                );

        int bookedRooms = overlappingBookings.stream()
                .mapToInt(Booking::getTotalRooms)
                .sum();

        int availableRooms = room.getTotalRooms() - bookedRooms;

        if (request.getTotalRooms() > availableRooms)
        {
            throw new RuntimeException(
                    "Only " + availableRooms + " room(s) are available for the selected dates"
            );
        }

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);

        BigDecimal pricePerNight = BigDecimal.valueOf(room.getRoomPrice());

        BigDecimal totalAmount = pricePerNight.multiply(BigDecimal.valueOf(nights)).multiply(BigDecimal.valueOf(request.getTotalRooms()));

        String bookingNumber;

        do
        {
            bookingNumber = "SE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        }
        while (
                bookingRepository.existsByBookingNumber(bookingNumber)
        );

        Booking booking = new Booking();

        booking.setBookingNumber(bookingNumber);
        booking.setCustomerId(customer.getUserId());
        booking.setCustomerName(customer.getUserName());
        booking.setCustomerEmail(customer.getUserEmail());
        booking.setHotelId(hotel.getHotelId());
        booking.setHotelName(hotel.getHotelName());
        booking.setRoomId(room.getRoomId());
        booking.setRoomType(room.getRoomType());
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setTotalGuests(request.getTotalGuests());
        booking.setTotalRooms(request.getTotalRooms());
        booking.setRoomPrice(pricePerNight);
        booking.setTotalAmount(totalAmount);
        booking.setBookingStatus(Booking.BookingStatus.PENDING_PAYMENT);
        booking.setPaymentStatus(Booking.PaymentStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());

        Booking savedBooking = bookingRepository.save(booking);

        reduceAvailableRooms(roomId, request.getTotalRooms());

        return savedBooking;
    }

    public void cancelBooking(String bookingId, String customerId)
    {
        Booking booking = getBookingById(bookingId);

        if (!customerId.equals(booking.getCustomerId()))
        {
            throw new RuntimeException("You are not allowed to cancel this booking");
        }

        if (booking.getBookingStatus() != Booking.BookingStatus.CONFIRMED)
        {
            throw new RuntimeException("This booking cannot be cancelled");
        }

        if (!LocalDate.now().isBefore(booking.getCheckInDate()))
        {
            throw new RuntimeException("Booking cannot be cancelled after check-in");
        }

        booking.setBookingStatus(Booking.BookingStatus.CANCELLED);

        Booking savedBooking = bookingRepository.save(booking);

        restoreAvailableRooms(booking.getRoomId(), booking.getTotalRooms());

        try
        {
            emailService.sendBookingCancellation(savedBooking);
        }
        catch (Exception exception)
        {
            System.out.println("Cancellation email failed: " + exception.getMessage());
        }
    }

    public void cancelBookingByManager(String bookingId, String hotelId)
    {
        Booking booking = getHotelBooking(bookingId, hotelId);

        if (booking.getBookingStatus() != Booking.BookingStatus.CONFIRMED)
        {
            throw new RuntimeException("This booking cannot be cancelled");
        }

        if (!LocalDate.now().isBefore(booking.getCheckInDate()))
        {
            throw new RuntimeException("Booking cannot be cancelled after check-in");
        }

        booking.setBookingStatus(Booking.BookingStatus.CANCELLED);

        bookingRepository.save(booking);

        restoreAvailableRooms(booking.getRoomId(), booking.getTotalRooms());
    }

    public void updateCompletedBookings()
    {
        List<Booking> bookings = bookingRepository.findAll();

        LocalDate today = LocalDate.now();

        for (Booking booking : bookings)
        {
            if (booking.getBookingStatus() == Booking.BookingStatus.CONFIRMED && booking.getCheckOutDate() != null && !booking.getCheckOutDate().isAfter(today))
            {
                booking.setBookingStatus(Booking.BookingStatus.COMPLETED);

                bookingRepository.save(booking);
            }
        }
    }

    public void cancelExpiredPendingPayments()
    {
        List<Booking> bookings = bookingRepository.findAll();

        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(60);

        for (Booking booking : bookings)
        {
            if (booking.getBookingStatus() == Booking.BookingStatus.PENDING_PAYMENT && booking.getCreatedAt() != null && booking.getCreatedAt().isBefore(expiryTime))
            {
                booking.setBookingStatus(Booking.BookingStatus.CANCELLED);
                booking.setPaymentStatus(Booking.PaymentStatus.FAILED);

                restoreAvailableRooms(booking.getRoomId(), booking.getTotalRooms());

                bookingRepository.save(booking);
            }
        }
    }

    public void reduceAvailableRooms(String roomId, int bookedRooms)
    {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new RuntimeException("Room not found"));

        if (room.getAvailableRooms() < bookedRooms)
        {
            throw new RuntimeException("Not enough rooms available");
        }

        room.setAvailableRooms(room.getAvailableRooms() - bookedRooms);

        roomRepository.save(room);
    }

    public void restoreAvailableRooms(String roomId, int bookedRooms)
    {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new RuntimeException("Room not found"));

        int available = room.getAvailableRooms() + bookedRooms;

        room.setAvailableRooms(Math.min(available, room.getTotalRooms()));

        roomRepository.save(room);
    }
}
