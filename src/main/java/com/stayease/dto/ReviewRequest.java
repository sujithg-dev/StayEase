package com.stayease.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewRequest
{
    @NotNull(message = "Please select a overall rating")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer overallRating;

    @NotNull(message = "Please select a cleanliness rating")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer cleanlinessRating;

    @NotNull(message = "Please select a comfort rating")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer comfortRating;

    @NotNull(message = "Please select a amenities rating")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer amenitiesRating;

    @NotNull(message = "Please select a location rating")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer locationRating;

    @NotBlank(message = "Comment is required")
    @Size(min = 5, max = 500, message = "Feedback must be between 5 and 500 characters")
    private String feedback;
}