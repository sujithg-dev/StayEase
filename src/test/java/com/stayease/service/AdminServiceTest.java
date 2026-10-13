package com.stayease.service;

import com.stayease.model.Booking;
import com.stayease.model.Hotel;
import com.stayease.model.User;
import com.stayease.repository.BookingRepository;
import com.stayease.repository.HotelRepository;
import com.stayease.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest
{
    @Mock
    private UserRepository userRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private AdminService adminService;

    private User customer;
    private User manager;
    private User admin;
    private Hotel activeHotel;
    private Hotel inactiveHotel;

    @BeforeEach
    void setUp()
    {
        customer = new User();
        customer.setUserId("user-1");
        customer.setUserRole(User.UserRole.CUSTOMER);
        customer.setUserStatus(User.UserStatus.ACTIVE);

        manager = new User();
        manager.setUserId("user-2");
        manager.setUserRole(User.UserRole.MANAGER);
        manager.setUserStatus(User.UserStatus.ACTIVE);

        admin = new User();
        admin.setUserId("admin-1");
        admin.setUserRole(User.UserRole.ADMIN);
        admin.setUserStatus(User.UserStatus.ACTIVE);

        activeHotel = new Hotel();
        activeHotel.setHotelId("hotel-1");
        activeHotel.setHotelStatus(Hotel.HotelStatus.ACTIVE);

        inactiveHotel = new Hotel();
        inactiveHotel.setHotelId("hotel-2");
        inactiveHotel.setHotelStatus(Hotel.HotelStatus.INACTIVE);
    }

    @Test
    void getAllUsersReturnsAllUsers()
    {
        when(userRepository.findAll()).thenReturn(List.of(customer, manager));

        assertEquals(2, adminService.getAllUsers().size());
        verify(userRepository).findAll();
    }

    @Test
    void getAllHotelsReturnsAllHotels()
    {
        when(hotelRepository.findAll()).thenReturn(List.of(activeHotel, inactiveHotel));

        assertEquals(2, adminService.getAllHotels().size());
        verify(hotelRepository).findAll();
    }

    @Test
    void getAllBookingsReturnsAllBookings()
    {
        Booking booking = new Booking();
        when(bookingRepository.findAll()).thenReturn(List.of(booking));

        assertEquals(1, adminService.getAllBookings().size());
        verify(bookingRepository).findAll();
    }

    @Test
    void getUserByIdReturnsUser()
    {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(customer));

        assertSame(customer, adminService.getUserById("user-1"));
    }

    @Test
    void getUserByIdThrowsWhenUserDoesNotExist()
    {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> adminService.getUserById("missing"));

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void getHotelByIdReturnsHotel()
    {
        when(hotelRepository.findById("hotel-1")).thenReturn(Optional.of(activeHotel));

        assertSame(activeHotel, adminService.getHotelById("hotel-1"));
    }

    @Test
    void getHotelByIdThrowsWhenHotelDoesNotExist()
    {
        when(hotelRepository.findById("missing")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> adminService.getHotelById("missing"));

        assertEquals("Hotel not found", exception.getMessage());
    }

    @Test
    void getUserCountReturnsRepositoryCount()
    {
        when(userRepository.count()).thenReturn(5L);

        assertEquals(5L, adminService.getUserCount());
    }

    @Test
    void getCustomerCountReturnsOnlyCustomers()
    {
        when(userRepository.findAll()).thenReturn(List.of(customer, manager, admin));

        assertEquals(1L, adminService.getCustomerCount());
    }

    @Test
    void getManagerCountReturnsOnlyManagers()
    {
        when(userRepository.findAll()).thenReturn(List.of(customer, manager, admin));

        assertEquals(1L, adminService.getManagerCount());
    }

    @Test
    void getHotelCountReturnsRepositoryCount()
    {
        when(hotelRepository.count()).thenReturn(3L);

        assertEquals(3L, adminService.getHotelCount());
    }

    @Test
    void getBookingCountReturnsRepositoryCount()
    {
        when(bookingRepository.count()).thenReturn(8L);

        assertEquals(8L, adminService.getBookingCount());
    }

    @Test
    void getActiveHotelCountReturnsActiveHotelCount()
    {
        when(hotelRepository.findByHotelStatus(Hotel.HotelStatus.ACTIVE)).thenReturn(List.of(activeHotel));

        assertEquals(1L, adminService.getActiveHotelCount());
    }

    @Test
    void getInactiveHotelCountReturnsInactiveHotelCount()
    {
        when(hotelRepository.findByHotelStatus(Hotel.HotelStatus.INACTIVE)).thenReturn(List.of(inactiveHotel));

        assertEquals(1L, adminService.getInactiveHotelCount());
    }

    @Test
    void getConfirmedBookingCountReturnsConfirmedBookings()
    {
        Booking confirmed = new Booking();
        confirmed.setBookingStatus(Booking.BookingStatus.CONFIRMED);

        Booking cancelled = new Booking();
        cancelled.setBookingStatus(Booking.BookingStatus.CANCELLED);

        when(bookingRepository.findAll()).thenReturn(List.of(confirmed, cancelled));

        assertEquals(1L, adminService.getConfirmedBookingCount());
    }

    @Test
    void getPendingPaymentCountReturnsPendingBookings()
    {
        Booking pending = new Booking();
        pending.setBookingStatus(Booking.BookingStatus.PENDING_PAYMENT);

        Booking confirmed = new Booking();
        confirmed.setBookingStatus(Booking.BookingStatus.CONFIRMED);

        when(bookingRepository.findAll()).thenReturn(List.of(pending, confirmed));

        assertEquals(1L, adminService.getPendingPaymentCount());
    }

    @Test
    void getCancelledBookingCountReturnsCancelledBookings()
    {
        Booking cancelled = new Booking();
        cancelled.setBookingStatus(Booking.BookingStatus.CANCELLED);

        Booking completed = new Booking();
        completed.setBookingStatus(Booking.BookingStatus.COMPLETED);

        when(bookingRepository.findAll()).thenReturn(List.of(cancelled, completed));

        assertEquals(1L, adminService.getCancelledBookingCount());
    }

    @Test
    void getCompletedBookingCountReturnsCompletedBookings()
    {
        Booking completed = new Booking();
        completed.setBookingStatus(Booking.BookingStatus.COMPLETED);

        Booking confirmed = new Booking();
        confirmed.setBookingStatus(Booking.BookingStatus.CONFIRMED);

        when(bookingRepository.findAll()).thenReturn(List.of(completed, confirmed));

        assertEquals(1L, adminService.getCompletedBookingCount());
    }

    @Test
    void changeUserStatusChangesActiveUserToInactive()
    {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(customer));

        adminService.changeUserStatus("user-1");

        assertEquals(User.UserStatus.INACTIVE, customer.getUserStatus());
        verify(userRepository).save(customer);
    }

    @Test
    void changeUserStatusChangesInactiveUserToActive()
    {
        customer.setUserStatus(User.UserStatus.INACTIVE);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(customer));

        adminService.changeUserStatus("user-1");

        assertEquals(User.UserStatus.ACTIVE, customer.getUserStatus());
        verify(userRepository).save(customer);
    }

    @Test
    void changeUserStatusRejectsAdminAccount()
    {
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(admin));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> adminService.changeUserStatus("admin-1"));

        assertEquals("Admin accounts cannot be disabled", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changeUserRoleChangesRole()
    {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(customer));

        adminService.changeUserRole("user-1", User.UserRole.MANAGER, "admin-1");

        assertEquals(User.UserRole.MANAGER, customer.getUserRole());
        verify(userRepository).save(customer);
    }

    @Test
    void changeUserRoleRejectsChangingOwnRole()
    {
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(admin));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> adminService.changeUserRole("admin-1", User.UserRole.MANAGER, "admin-1"));

        assertEquals("You cannot change your own role", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changeUserRoleRejectsNullRole()
    {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(customer));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> adminService.changeUserRole("user-1", null, "admin-1"));

        assertEquals("Invalid user role", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changeUserRoleRejectsDemotingLastAdmin()
    {
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(admin));
        when(userRepository.countByUserRole(User.UserRole.ADMIN)).thenReturn(1L);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> adminService.changeUserRole("admin-1", User.UserRole.CUSTOMER, "another-admin"));

        assertEquals("The last administrator cannot be changed to another role", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changeUserRoleAllowsDemotingAdminWhenAnotherAdminExists()
    {
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(admin));
        when(userRepository.countByUserRole(User.UserRole.ADMIN)).thenReturn(2L);

        adminService.changeUserRole("admin-1", User.UserRole.MANAGER, "another-admin");

        assertEquals(User.UserRole.MANAGER, admin.getUserRole());
        verify(userRepository).save(admin);
    }

    @Test
    void changeHotelStatusChangesActiveHotelToInactive()
    {
        when(hotelRepository.findById("hotel-1")).thenReturn(Optional.of(activeHotel));

        adminService.changeHotelStatus("hotel-1");

        assertEquals(Hotel.HotelStatus.INACTIVE, activeHotel.getHotelStatus());
        verify(hotelRepository).save(activeHotel);
    }

    @Test
    void changeHotelStatusChangesInactiveHotelToActive()
    {
        when(hotelRepository.findById("hotel-2")).thenReturn(Optional.of(inactiveHotel));

        adminService.changeHotelStatus("hotel-2");

        assertEquals(Hotel.HotelStatus.ACTIVE, inactiveHotel.getHotelStatus());
        verify(hotelRepository).save(inactiveHotel);
    }
}