package com.stayease.repository;

import com.stayease.model.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String>
{
    List<Review> findByHotelIdOrderByCreatedAtDesc(String hotelId);

    boolean existsByBookingIdAndCustomerId(String bookingId, String customerId);
}