package com.stayease.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review
{
    @Id
    private String reviewId;

    private String customerId;

    private String customerName;

    private String hotelId;

    private String hotelName;

    private String bookingId;

    private Integer overallRating;

    private Integer cleanlinessRating;

    private Integer comfortRating;

    private Integer amenitiesRating;

    private Integer locationRating;

    private String feedback;

    private LocalDateTime createdAt;
}