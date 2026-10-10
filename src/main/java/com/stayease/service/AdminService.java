package com.stayease.service;

import com.stayease.model.Booking;
import com.stayease.model.Hotel;
import com.stayease.model.User;
import com.stayease.repository.BookingRepository;
import com.stayease.repository.HotelRepository;
import com.stayease.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService
{
    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;

    public List<User> getAllUsers()
    {
        return userRepository.findAll();
    }

    public List<Hotel> getAllHotels()
    {
        return hotelRepository.findAll();
    }

    public List<Booking> getAllBookings()
    {
        return bookingRepository.findAll();
    }

    public User getUserById(String userId)
    {
        return userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Hotel getHotelById(String hotelId)
    {
        return hotelRepository.findById(hotelId).orElseThrow(() -> new RuntimeException("Hotel not found"));
    }

    public long getUserCount()
    {
        return userRepository.count();
    }

    public long getCustomerCount()
    {
        return userRepository.findAll().stream().filter(user -> user.getUserRole() == User.UserRole.CUSTOMER).count();
    }

    public long getManagerCount()
    {
        return userRepository.findAll().stream().filter(user -> user.getUserRole() == User.UserRole.MANAGER).count();
    }

    public long getHotelCount()
    {
        return hotelRepository.count();
    }

    public long getBookingCount()
    {
        return bookingRepository.count();
    }

    public long getActiveHotelCount()
    {
        return hotelRepository.findByHotelStatus(Hotel.HotelStatus.ACTIVE).size();
    }

    public long getInactiveHotelCount()
    {
        return hotelRepository.findByHotelStatus(Hotel.HotelStatus.INACTIVE).size();
    }

    public long getConfirmedBookingCount()
    {
        return bookingRepository.findAll().stream().filter(booking -> booking.getBookingStatus() == Booking.BookingStatus.CONFIRMED).count();
    }

    public long getPendingPaymentCount()
    {
        return bookingRepository.findAll().stream().filter(booking -> booking.getBookingStatus() == Booking.BookingStatus.PENDING_PAYMENT).count();
    }

    public long getCancelledBookingCount()
    {
        return bookingRepository.findAll().stream().filter(booking -> booking.getBookingStatus() == Booking.BookingStatus.CANCELLED).count();
    }

    public long getCompletedBookingCount()
    {
        return bookingRepository.findAll().stream().filter(booking -> booking.getBookingStatus() == Booking.BookingStatus.COMPLETED).count();
    }

    public void changeUserStatus(String userId)
    {
        User user = getUserById(userId);

        if (user.getUserRole() == User.UserRole.ADMIN)
        {
            throw new RuntimeException("Admin accounts cannot be disabled");
        }

        if (user.getUserStatus() == User.UserStatus.ACTIVE)
        {
            user.setUserStatus(User.UserStatus.INACTIVE);
        }
        else
        {
            user.setUserStatus(User.UserStatus.ACTIVE);
        }

        userRepository.save(user);
    }

    public void changeUserRole(String userId, User.UserRole newRole, String adminUserId)
    {
        User user = getUserById(userId);

        if (user.getUserId().equals(adminUserId))
        {
            throw new RuntimeException("You cannot change your own role");
        }

        if (newRole == null)
        {
            throw new RuntimeException("Invalid user role");
        }

        if (user.getUserRole() == User.UserRole.ADMIN && newRole != User.UserRole.ADMIN)
        {
            long adminCount = userRepository.countByUserRole(User.UserRole.ADMIN);

            if (adminCount <= 1)
            {
                throw new RuntimeException("The last administrator cannot be changed to another role");
            }
        }

        user.setUserRole(newRole);

        userRepository.save(user);
    }

    public void changeHotelStatus(String hotelId)
    {
        Hotel hotel = getHotelById(hotelId);

        if (hotel.getHotelStatus() == Hotel.HotelStatus.ACTIVE)
        {
            hotel.setHotelStatus(Hotel.HotelStatus.INACTIVE);
        }
        else
        {
            hotel.setHotelStatus(Hotel.HotelStatus.ACTIVE);
        }

        hotelRepository.save(hotel);
    }
}
