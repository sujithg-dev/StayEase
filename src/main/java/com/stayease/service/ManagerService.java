package com.stayease.service;

import com.stayease.helper.MongoTable;
import com.stayease.model.Booking;
import com.stayease.model.Hotel;
import com.stayease.repository.BookingRepository;
import com.stayease.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


@Service
@RequiredArgsConstructor
public class ManagerService
{
    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;

    private final MongoTemplate mongoTemplate;

    public long getHotelCountByManageId(String managerId)
    {
        return hotelRepository.countByManagerId(managerId);
    }

    public long getActiveHotelCountByManageId(String managerId)
    {
        return hotelRepository.countByManagerIdAndHotelStatus(managerId, Hotel.HotelStatus.ACTIVE);
    }

    public long getInactiveHotelCountByManageId(String managerId)
    {
        return hotelRepository.countByManagerIdAndHotelStatus(managerId, Hotel.HotelStatus.INACTIVE);
    }

    public long getBookingCountByManageId(String managerId)
    {
        List<String> hotelIds = getManagerHotelIds(managerId);
        return hotelIds.isEmpty() ? 0 : bookingRepository.countByHotelIdIn(hotelIds);
    }

    public long getConfirmedBookingCountByManageId(String managerId)
    {
        List<String> hotelIds = getManagerHotelIds(managerId);
        return hotelIds.isEmpty() ? 0 : bookingRepository.countByHotelIdInAndBookingStatus(hotelIds, Booking.BookingStatus.CONFIRMED);
    }

    public long getPendingPaymentCountByManageId(String managerId)
    {
        List<String> hotelIds = getManagerHotelIds(managerId);
        return hotelIds.isEmpty() ? 0 : bookingRepository.countByHotelIdInAndBookingStatus(hotelIds, Booking.BookingStatus.PENDING_PAYMENT);
    }

    public long getCancelledBookingCountByManageId(String managerId)
    {
        List<String> hotelIds = getManagerHotelIds(managerId);
        return hotelIds.isEmpty() ? 0 : bookingRepository.countByHotelIdInAndBookingStatus(hotelIds, Booking.BookingStatus.CANCELLED);
    }

    public long getCompletedBookingCountByManageId(String managerId)
    {
        List<String> hotelIds = getManagerHotelIds(managerId);
        return hotelIds.isEmpty() ? 0 : bookingRepository.countByHotelIdInAndBookingStatus(hotelIds, Booking.BookingStatus.COMPLETED);
    }

    public List<Hotel> getManagerHotelsByManageId(String managerId)
    {
        return hotelRepository.findByManagerId(managerId);
    }

    public boolean isHotelOwnedByManager(String hotelId, String managerId)
    {
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new RuntimeException("Hotel not found"));

        return hotel.getManagerId().equals(managerId);
    }

    private List<String> getManagerHotelIds(String managerId)
    {
        return hotelRepository.findByManagerId(managerId)
                .stream()
                .map(Hotel::getHotelId)
                .toList();
    }

    public Page<Booking> getManagerHotelBookings(String managerId, String q, String hotel, String payment, String status, LocalDate from, LocalDate to, Pageable pageable)
    {
        List<Hotel> hotels = mongoTemplate.find(Query.query(Criteria.where("managerId").is(managerId)), Hotel.class);

        List<String> hotelIds = hotels.stream().map(Hotel::getHotelId).toList();

        if (hotelIds.isEmpty())
        {
            return Page.empty(pageable);
        }

        List<Criteria> criteria = Arrays.asList(
                Criteria.where("hotelId").in(hotelIds),
                MongoTable.search(q, "bookingNumber", "customerName", "customerEmail", "hotelName", "roomName"),
                MongoTable.equal("hotelName", hotel),
                MongoTable.enumEqual("paymentStatus", Booking.PaymentStatus.class, payment),
                MongoTable.enumEqual("bookingStatus", Booking.BookingStatus.class, status),
                MongoTable.dateFrom("checkInDate", from),
                MongoTable.dateTo("checkInDate", to));

        return MongoTable.find(mongoTemplate, Booking.class, criteria, pageable);
    }

    public List<String> getManagerHotelNames(String managerId)
    {
        return mongoTemplate.find(Query.query(Criteria.where("managerId").is(managerId)), Hotel.class)
                .stream()
                .map(Hotel::getHotelName)          // use your actual getter
                .filter(Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}