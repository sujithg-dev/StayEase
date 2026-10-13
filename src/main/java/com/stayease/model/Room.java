package com.stayease.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room
{
    @Id
    private String roomId;

    private String hotelId;

    @NotBlank(message = "Room name is required")
    @Size(min = 5, max = 30, message = "Room name must be between 3 and 30 characters")
    private String roomName;

    @NotBlank(message = "Room type is required")
    private String roomType;

    @NotBlank(message = "Room description is required")
    private String roomDescription;

    @NotNull(message = "Room Price is required")
    @DecimalMin(value = "1.0", message = "Price must be at least 1")
    private Double roomPrice;

    @NotNull(message = "Room Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer roomCapacity;

    @NotNull(message = "Total rooms is required")
    @Min(value = 1, message = "Total rooms must be at least 1")
    private Integer totalRooms;

    @NotNull(message = "Available rooms is required")
    @Min(value = 0, message = "Available rooms cannot be negative")
    private Integer availableRooms;

    private RoomStatus roomStatus = RoomStatus.AVAILABLE;

    private String roomCoverImage;

    private List<String> roomImages = new ArrayList<>();

    public enum RoomStatus
    {
        AVAILABLE,
        UNAVAILABLE
    }
}