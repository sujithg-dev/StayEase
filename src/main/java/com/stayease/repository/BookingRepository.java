package com.stayease.repository;

import com.stayease.model.Booking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String>
{
    List<Booking> findByRoomIdAndBookingStatusAndCheckInDateLessThanAndCheckOutDateGreaterThan(
            String roomId,
            List<Booking.BookingStatus> bookingStatuses,
            LocalDate checkOutDate,
            LocalDate checkInDate
    );

    List<Booking> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<Booking> findByHotelIdOrderByCreatedAtDesc(String hotelId);

    boolean existsByBookingNumber(String bookingNumber);

    long countByHotelIdIn(List<String> hotelIds);

    long countByHotelIdInAndBookingStatus(List<String> hotelIds, Booking.BookingStatus bookingStatus);
}
