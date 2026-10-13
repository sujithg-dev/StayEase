package com.stayease.service;

import com.stayease.helper.MongoTable;
import com.stayease.model.Booking;
import com.stayease.model.Hotel;
import com.stayease.model.User;
import com.stayease.repository.BookingRepository;
import com.stayease.repository.HotelRepository;
import com.stayease.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService
{
    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;

    private final MongoTemplate mongoTemplate;

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

    public Page<User> getUsers(String q, String role, String status, Pageable pageable)
    {
        List<Criteria> criteria = Arrays.asList(
                MongoTable.search(q, "userName", "userEmail"),
                MongoTable.enumEqual("userRole", User.UserRole.class, role),
                MongoTable.enumEqual("userStatus", User.UserStatus.class, status),
                Criteria.where("userRole").ne(User.UserRole.ADMIN));

        return MongoTable.find(mongoTemplate, User.class, criteria, pageable);
    }

    public Page<Hotel> getHotels(String q, String status, Pageable pageable)
    {
        List<Criteria> criteria = Arrays.asList(
                MongoTable.search(q, "hotelName", "managerName", "mangerEmail", "hotelCity", "hotelState", "hotelCountry"),
                MongoTable.enumEqual("hotelStatus", Hotel.HotelStatus.class, status));

        Page<Hotel> page = MongoTable.find(mongoTemplate, Hotel.class, criteria, pageable);

        Map<String, String> names = getManagerNames(page.getContent());
        Map<String, String> email = getManagerEmails(page.getContent());
        page.getContent().forEach(h -> h.setManagerName(names.get(h.getManagerId())));
        page.getContent().forEach(h -> h.setManagerEmail(email.get(h.getManagerId())));

        return page;
    }

    public Map<String, String> getManagerNames(Collection<Hotel> hotels)
    {
        Set<String> ids = hotels.stream()
                .map(Hotel::getManagerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) return Map.of();

        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(
                        User::getUserId,
                        u -> Objects.toString(u.getUserName(), "")));
    }

    public Map<String, String> getManagerEmails(Collection<Hotel> hotels)
    {
        Set<String> ids = hotels.stream()
                .map(Hotel::getManagerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) return Map.of();

        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(
                        User::getUserId,
                        u -> Objects.toString(u.getUserEmail(), "")));
    }

    public List<String> getBookingHotelNames() {
        return mongoTemplate.findDistinct(new Query(), "hotelName", Booking.class, String.class)
                .stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    public Page<Booking> getBookings(String q, String hotel, String payment, String status, LocalDate from, LocalDate to, Pageable pageable)
    {
        List<Criteria> criteria = Arrays.asList(
                MongoTable.search(q, "bookingNumber", "customerName", "customerEmail", "hotelName", "roomName"),
                MongoTable.equal("hotelName", hotel),
                MongoTable.enumEqual("paymentStatus", Booking.PaymentStatus.class, payment),
                MongoTable.enumEqual("bookingStatus", Booking.BookingStatus.class, status),
                MongoTable.dateFrom("checkInDate", from),
                MongoTable.dateTo("checkInDate", to));

        return MongoTable.find(mongoTemplate, Booking.class, criteria, pageable);
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
