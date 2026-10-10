package com.stayease.dto;

import lombok.Data;

import java.util.List;

@Data
public class HotelSearchRequest
{
        private String city;

        private Double minPrice;

        private Double maxPrice;

        private Integer guests;

        private List<String> amenities;

        private Double minRating;
}