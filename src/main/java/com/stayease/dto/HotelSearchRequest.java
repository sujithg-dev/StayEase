package com.stayease.dto;

import lombok.Data;

import java.util.List;

@Data
public class HotelSearchRequest
{
        private String destination;

        private Double minPrice;

        private Double maxPrice;

        private Double minRating;

        private Integer guests;

        private List<String> amenities;
}