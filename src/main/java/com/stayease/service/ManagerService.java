package com.stayease.service;

import com.stayease.model.Booking;
import com.stayease.model.Hotel;
import com.stayease.repository.BookingRepository;
import com.stayease.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class ManagerService
{
    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;

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
}