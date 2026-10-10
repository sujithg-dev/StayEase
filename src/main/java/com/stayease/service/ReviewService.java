package com.stayease.service;

import com.stayease.dto.ReviewRequest;
import com.stayease.model.Booking;
import com.stayease.model.Hotel;
import com.stayease.model.Review;
import com.stayease.repository.BookingRepository;
import com.stayease.repository.HotelRepository;
import com.stayease.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.ToIntFunction;

@Service
@RequiredArgsConstructor
public class ReviewService
{
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;

    public List<Review> getHotelReviews(String hotelId)
    {
        return reviewRepository.findByHotelIdOrderByCreatedAtDesc(hotelId);
    }

    public Review createReview(String customerId, String bookingId, ReviewRequest request)
    {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getCustomerId().equals(customerId))
        {
            throw new RuntimeException("You are not allowed to review this booking");
        }

        if (booking.getBookingStatus() != Booking.BookingStatus.CONFIRMED && booking.getBookingStatus() != Booking.BookingStatus.COMPLETED)
        {
            throw new RuntimeException("Only confirmed bookings can be reviewed");
        }

        if (booking.getCheckOutDate().isAfter(LocalDate.now()))
        {
            throw new RuntimeException("You can review the hotel after your stay");
        }

        if (reviewRepository.existsByBookingIdAndCustomerId(bookingId, customerId))
        {
            throw new RuntimeException("You have already reviewed this booking");
        }

        Review review = new Review();

        review.setCustomerId(customerId);
        review.setCustomerName(booking.getCustomerName());
        review.setHotelId(booking.getHotelId());
        review.setHotelName(booking.getHotelName());
        review.setBookingId(bookingId);
        review.setOverallRating((request.getOverallRating()));
        review.setCleanlinessRating((request.getCleanlinessRating()));
        review.setAmenitiesRating((request.getAmenitiesRating()));
        review.setLocationRating((request.getLocationRating()));
        review.setComfortRating((request.getComfortRating()));
        review.setFeedback(request.getFeedback());
        review.setCreatedAt(LocalDateTime.now());
        Review savedReview = reviewRepository.save(review);

        updateHotelRating(booking.getHotelId());

        return savedReview;
    }

    public boolean canReview(String customerId, String bookingId)
    {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);

        if (booking == null)
        {
            return false;
        }

        if (!booking.getCustomerId().equals(customerId))
        {
            return false;
        }

        if (booking.getBookingStatus() != Booking.BookingStatus.CONFIRMED && booking.getBookingStatus() != Booking.BookingStatus.COMPLETED) {

            return false;
        }

        if (booking.getCheckOutDate() == null || booking.getCheckOutDate().isAfter(LocalDate.now()))
        {
            return false;
        }

        return !reviewRepository.existsByBookingIdAndCustomerId(bookingId, customerId);
    }

    private void updateHotelRating(String hotelId)
    {
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new RuntimeException("Hotel not found"));

        List<Review> reviews = reviewRepository.findByHotelIdOrderByCreatedAtDesc(hotelId);

        if (reviews.isEmpty())
        {
            return;
        }

        hotel.setHotelRating(averageRating(reviews, Review::getOverallRating));
        hotel.setHotelClenlinessRating(averageRating(reviews, Review::getCleanlinessRating));
        hotel.setHotelAmenitiesRating(averageRating(reviews, Review::getAmenitiesRating));
        hotel.setHotelComfortRating(averageRating(reviews, Review::getComfortRating));
        hotel.setHotelLocationRating(averageRating(reviews, Review::getLocationRating));

        hotelRepository.save(hotel);
    }

    private double averageRating(List<Review> reviews, ToIntFunction<Review> rating)
    {
        return Math.round(reviews.stream().mapToInt(rating).average().orElse(0.0) * 10.0) / 10.0;
    }
}