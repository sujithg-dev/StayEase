package com.stayease.repository;

import com.stayease.model.Hotel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HotelRepository extends MongoRepository<Hotel, String>
{
    List<Hotel> findByManagerId(String managerId);

    List<Hotel> findByHotelStatus(Hotel.HotelStatus hotelStatus);

    long countByManagerId(String managerId);

    long countByManagerIdAndHotelStatus(String managerId, Hotel.HotelStatus hotelStatus);
}