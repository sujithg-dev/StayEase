package com.stayease.model;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "hotels")
@Data
public class Hotel
{
    @Id
    private String hotelId;

    private String managerId;

    private String managerName;

    private String managerEmail;

    @NotBlank(message = "Hotel name is required")
    private String hotelName;

    @NotBlank(message = "Hotel description is required")
    private String hotelDescription;

    @NotBlank(message = "Street is required")
    private String hotelStreet;

    @NotBlank(message = "City is required")
    private String hotelCity;

    @NotBlank(message = "State is required")
    private String hotelState;

    @NotBlank(message = "Pincode is required")
    private String hotelPincode;

    @NotBlank(message = "Country is required")
    private String hotelCountry;

    private String hotelPhone;

    @Email(message = "Enter a valid email")
    private String hotelEmail;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Double hotelRating;

    private Double hotelClenlinessRating = 1.0;

    private Double hotelComfortRating = 1.0;

    private Double hotelAmenitiesRating = 1.0;

    private Double hotelLocationRating = 1.0;

    private HotelStatus hotelStatus = HotelStatus.ACTIVE;

    private List<String> hotelAmenities = new ArrayList<>();

    private List<String> hotelPolicies = new ArrayList<>();

    private String hotelCoverImage;

    private List<String> hotelImages = new ArrayList<>();

    @Transient
    private Double startingPrice;

    public enum HotelStatus
    {
        ACTIVE,
        INACTIVE
    }

    @Getter
    public enum HotelAmenity
    {
        WIFI("Wi-Fi"),
        AC("AC"),
        PARKING("Parking"),
        RESTAURANT("Restaurant"),
        BREAKFAST("Breakfast"),
        SWIMMING_POOL("Swimming Pool"),
        GYM("Gym"),
        ROOM_SERVICE("Room Service"),
        SPA("Spa"),
        LAUNDRY("Laundry"),
        PET_FRIENDLY("Pet Friendly"),
        FRONT_DESK_24_HOURS("24Hr Front Desk"),
        BEACH_ACCESS("Beach Access");

        private final String displayName;

        HotelAmenity(String displayName)
        {
            this.displayName = displayName;
        }

    }
}