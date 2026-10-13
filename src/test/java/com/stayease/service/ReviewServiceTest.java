
package com.stayease.service;

import com.stayease.dto.ReviewRequest;
import com.stayease.model.Booking;
import com.stayease.model.Hotel;
import com.stayease.model.Review;
import com.stayease.repository.BookingRepository;
import com.stayease.repository.HotelRepository;
import com.stayease.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest
{
    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private HotelRepository hotelRepository;

    @InjectMocks
    private ReviewService reviewService;

    private Booking booking;
    private Hotel hotel;
    private ReviewRequest request;

    @BeforeEach
    void setUp()
    {
        booking = new Booking();
        booking.setBookingId("booking123");
        booking.setCustomerId("customer123");
        booking.setCustomerName("Test Customer");
        booking.setHotelId("hotel123");
        booking.setHotelName("Coastal Pearl Resort");
        booking.setBookingStatus(Booking.BookingStatus.COMPLETED);
        booking.setCheckOutDate(LocalDate.now().minusDays(1));

        hotel = new Hotel();
        hotel.setHotelId("hotel123");
        hotel.setHotelName("Coastal Pearl Resort");

        request = new ReviewRequest();
        request.setOverallRating(5);
        request.setCleanlinessRating(4);
        request.setAmenitiesRating(5);
        request.setLocationRating(4);
        request.setComfortRating(5);
        request.setFeedback("Excellent stay!");
    }

    @Test
    void getHotelReviews_returnsReviews()
    {
        Review review = new Review();
        review.setHotelId("hotel123");

        when(reviewRepository.findByHotelIdOrderByCreatedAtDesc("hotel123")).thenReturn(List.of(review));

        List<Review> result = reviewService.getHotelReviews("hotel123");

        assertEquals(1, result.size());
        assertSame(review, result.getFirst());
        verify(reviewRepository).findByHotelIdOrderByCreatedAtDesc("hotel123");
    }

    @Test
    void createReview_whenBookingDoesNotExist_throwsException()
    {
        when(bookingRepository.findById("booking123")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reviewService.createReview("customer123", "booking123", request));

        assertEquals("Booking not found", exception.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void createReview_whenCustomerDoesNotOwnBooking_throwsException()
    {
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(booking));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reviewService.createReview("otherCustomer", "booking123", request));

        assertEquals("You are not allowed to review this booking", exception.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void createReview_whenBookingIsCancelled_throwsException()
    {
        booking.setBookingStatus(Booking.BookingStatus.CANCELLED);

        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(booking));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reviewService.createReview("customer123", "booking123", request));

        assertEquals("Only confirmed bookings can be reviewed", exception.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void createReview_whenCheckoutIsInFuture_throwsException()
    {
        booking.setCheckOutDate(LocalDate.now().plusDays(1));

        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(booking));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reviewService.createReview("customer123", "booking123", request));

        assertEquals("You can review the hotel after your stay", exception.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void createReview_whenReviewAlreadyExists_throwsException()
    {
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingIdAndCustomerId("booking123", "customer123")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reviewService.createReview("customer123", "booking123", request));

        assertEquals("You have already reviewed this booking", exception.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void createReview_whenValidRequest_savesReviewAndUpdatesHotelRating()
    {
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingIdAndCustomerId("booking123", "customer123")).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(hotelRepository.findById("hotel123")).thenReturn(Optional.of(hotel));
        when(reviewRepository.findByHotelIdOrderByCreatedAtDesc("hotel123"))
                .thenAnswer(invocation -> {
                    Review saved = new Review();
                    saved.setOverallRating(5);
                    saved.setCleanlinessRating(4);
                    saved.setAmenitiesRating(5);
                    saved.setComfortRating(5);
                    saved.setLocationRating(4);
                    return List.of(saved);
                });

        Review result = reviewService.createReview("customer123", "booking123", request);

        assertEquals("customer123", result.getCustomerId());
        assertEquals("Test Customer", result.getCustomerName());
        assertEquals("hotel123", result.getHotelId());
        assertEquals("Coastal Pearl Resort", result.getHotelName());
        assertEquals("booking123", result.getBookingId());
        assertEquals(5, result.getOverallRating());
        assertEquals(4, result.getCleanlinessRating());
        assertEquals(5, result.getAmenitiesRating());
        assertEquals(4, result.getLocationRating());
        assertEquals(5, result.getComfortRating());
        assertEquals("Excellent stay!", result.getFeedback());
        assertNotNull(result.getCreatedAt());

        assertEquals(5.0, hotel.getHotelRating());
        assertEquals(4.0, hotel.getHotelClenlinessRating());
        assertEquals(5.0, hotel.getHotelAmenitiesRating());
        assertEquals(5.0, hotel.getHotelComfortRating());
        assertEquals(4.0, hotel.getHotelLocationRating());

        verify(reviewRepository).save(any(Review.class));
        verify(hotelRepository).save(hotel);
    }

    @Test
    void createReview_whenHotelDoesNotExist_throwsExceptionDuringRatingUpdate()
    {
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingIdAndCustomerId("booking123", "customer123")).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(hotelRepository.findById("hotel123")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reviewService.createReview("customer123", "booking123", request));

        assertEquals("Hotel not found", exception.getMessage());
        verify(reviewRepository).save(any(Review.class));
        verify(hotelRepository, never()).save(any(Hotel.class));
    }

    @Test
    void canReview_whenBookingDoesNotExist_returnsFalse()
    {
        when(bookingRepository.findById("missing")).thenReturn(Optional.empty());

        assertFalse(reviewService.canReview("customer123", "missing"));
    }

    @Test
    void canReview_whenCustomerDoesNotOwnBooking_returnsFalse()
    {
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(booking));

        assertFalse(reviewService.canReview("otherCustomer", "booking123"));
        verifyNoInteractions(reviewRepository);
    }

    @Test
    void canReview_whenBookingIsCancelled_returnsFalse()
    {
        booking.setBookingStatus(Booking.BookingStatus.CANCELLED);

        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(booking));

        assertFalse(reviewService.canReview("customer123", "booking123"));
        verifyNoInteractions(reviewRepository);
    }

    @Test
    void canReview_whenCheckoutDateIsNull_returnsFalse()
    {
        booking.setCheckOutDate(null);

        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(booking));

        assertFalse(reviewService.canReview("customer123", "booking123"));
        verifyNoInteractions(reviewRepository);
    }

    @Test
    void canReview_whenCheckoutIsInFuture_returnsFalse()
    {
        booking.setCheckOutDate(LocalDate.now().plusDays(1));

        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(booking));

        assertFalse(reviewService.canReview("customer123", "booking123"));
        verifyNoInteractions(reviewRepository);
    }

    @Test
    void canReview_whenReviewAlreadyExists_returnsFalse()
    {
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingIdAndCustomerId("booking123", "customer123")).thenReturn(true);

        assertFalse(reviewService.canReview("customer123", "booking123"));
    }

    @Test
    void canReview_whenEligibleAndNoExistingReview_returnsTrue()
    {
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingIdAndCustomerId("booking123", "customer123")).thenReturn(false);

        assertTrue(reviewService.canReview("customer123", "booking123"));
    }
}
