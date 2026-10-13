package com.stayease.service;

import com.stayease.dto.HotelSearchRequest;
import com.stayease.model.Hotel;
import com.stayease.model.Room;
import com.stayease.repository.HotelRepository;
import com.stayease.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HotelService
{
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final RoomService roomService;
    private final ImageService imageService;
    private final MongoTemplate mongoTemplate;

    public List<Hotel> getAllHotels()
    {
        return hotelRepository.findAll();
    }

    public Page<Hotel> searchHotels(HotelSearchRequest request, int page, int size, String sortBy)
    {
        List<Hotel> hotels = hotelRepository.findByHotelStatus(Hotel.HotelStatus.ACTIVE);

        hotels = hotels
                .stream()
                .filter(hotel ->
                {
                    if (request.getDestination() == null || request.getDestination().isBlank()) return true;

                    String keyword = request.getDestination().trim().toLowerCase();

                    return (hotel.getHotelStreet() != null && hotel.getHotelStreet().toLowerCase().contains(keyword))
                            || (hotel.getHotelCity() != null && hotel.getHotelCity().toLowerCase().contains(keyword))
                            || (hotel.getHotelState() != null && hotel.getHotelState().toLowerCase().contains(keyword))
                            || (hotel.getHotelCountry() != null && hotel.getHotelCountry().toLowerCase().contains(keyword));
                })
                .filter(hotel ->
                {
                    if (request.getMinRating() == null) return true;

                    return hotel.getHotelRating() >= request.getMinRating();
                })
                .filter(hotel ->
                {
                    if (request.getAmenities() == null || request.getAmenities().isEmpty()) return true;

                    if (hotel.getHotelAmenities() == null) return false;

                    return request.getAmenities()
                            .stream()
                            .allMatch(selectedAmenity -> hotel.getHotelAmenities()
                                    .stream().anyMatch(hotelAmenity -> hotelAmenity.equalsIgnoreCase(selectedAmenity))
                            );
                })
                .filter(hotel ->
                {
                    List<Room> rooms = roomRepository.findByHotelId(hotel.getHotelId());

                    return rooms.stream().anyMatch(room ->
                    {
                        if (room.getRoomStatus() != Room.RoomStatus.AVAILABLE) return false;
                        if (room.getAvailableRooms() <= 0) return false;
                        if (request.getMinPrice() != null && room.getRoomPrice() < request.getMinPrice()) return false;
                        if (request.getMaxPrice() != null && room.getRoomPrice() > request.getMaxPrice()) return false;
                        if (request.getGuests() != null && room.getRoomCapacity() < request.getGuests()) return false;

                        return true;
                    });
                })
                .collect(Collectors.toList());

        hotels = new ArrayList<>(hotels);

        for (Hotel hotel : hotels)
        {
            Double startingPrice = roomService.getStartingPrice(hotel.getHotelId());
            Double highestPrice = roomService.getHighestrice(hotel.getHotelId());
            hotel.setStartingPrice(startingPrice);
            hotel.setHighestPrice(highestPrice);
        }

        switch (sortBy)
        {
            case "recommended" -> hotels.sort(Comparator.comparing(Hotel::getHotelRating, Comparator.nullsLast(Comparator.reverseOrder())));
            case "priceDesc" -> hotels.sort(Comparator.comparing(Hotel::getHighestPrice, Comparator.nullsLast(Comparator.reverseOrder())));
            case "priceAsc" -> hotels.sort(Comparator.comparing(Hotel::getStartingPrice, Comparator.nullsLast(Comparator.naturalOrder())));
            case "ratingDesc" -> hotels.sort(Comparator.comparing(Hotel::getHotelRating, Comparator.nullsLast(Comparator.reverseOrder())));
            case "ratingAsc" -> hotels.sort(Comparator.comparing(Hotel::getHotelRating, Comparator.nullsLast(Comparator.naturalOrder())));
            case "nameAsc" -> hotels.sort(Comparator.comparing(Hotel::getHotelName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
            case "nameDesc" -> hotels.sort(Comparator.comparing(Hotel::getHotelName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)).reversed());
            default -> hotels.sort(Comparator.comparing(Hotel::getHotelRating, Comparator.nullsLast(Comparator.reverseOrder())));
        }

        int totalElements = hotels.size();

        int start = page * size;
        int end = Math.min(start + size, totalElements);

        List<Hotel> paginatedHotels;

        if (start >= totalElements) paginatedHotels = List.of();
        else paginatedHotels = hotels.subList(start, end);

        Pageable pageable = PageRequest.of(page, size);

        return new PageImpl<>(paginatedHotels, pageable, totalElements);
    }

    public Hotel getHotelById(String id)
    {
        return hotelRepository.findById(id).orElseThrow(() -> new RuntimeException("Hotel not found!"));
    }

    public void createHotel(Hotel hotel, String managerId)
    {
        hotel.setManagerId(managerId);
        hotel.setHotelStatus(Hotel.HotelStatus.ACTIVE);

        if (hotel.getHotelRating() < 0)
        {
            hotel.setHotelRating(0.0);
        }

        if (hotel.getHotelRating() > 5)
        {
            hotel.setHotelRating(5.0);
        }

        hotelRepository.save(hotel);
    }

    public void updateHotel(String id, Hotel updatedHotel, String managerId)
    {
        Hotel existingHotel = getHotelById(id);

        if (existingHotel == null)
            throw new RuntimeException("Hotel not found");

        if (!existingHotel.getManagerId().equals(managerId))
            throw new RuntimeException("You are not authorized to modify this hotel");

        existingHotel.setHotelName(updatedHotel.getHotelName());
        existingHotel.setHotelStatus(updatedHotel.getHotelStatus());
        existingHotel.setHotelDescription(updatedHotel.getHotelDescription());
        existingHotel.setHotelStreet(updatedHotel.getHotelStreet());
        existingHotel.setHotelCity(updatedHotel.getHotelCity());
        existingHotel.setHotelState(updatedHotel.getHotelState());
        existingHotel.setHotelPincode(updatedHotel.getHotelPincode());
        existingHotel.setHotelCountry(updatedHotel.getHotelCountry());
        existingHotel.setHotelPhone(updatedHotel.getHotelPhone());
        existingHotel.setHotelEmail(updatedHotel.getHotelEmail());
        existingHotel.setHotelRating(updatedHotel.getHotelRating());
        existingHotel.setHotelAmenities(updatedHotel.getHotelAmenities());
        existingHotel.setHotelPolicies(updatedHotel.getHotelPolicies());

        hotelRepository.save(existingHotel);
    }

    public void deleteHotel(String id, String managerId)
    {
        Hotel hotel = getHotelById(id);

        if (!hotel.getManagerId().equals(managerId))
        {
            throw new RuntimeException("You are not authorized to delete this hotel");
        }

        hotelRepository.delete(hotel);
    }

    public void addHotelImage(String id, Hotel updatedHotel, String managerId)
    {
        Hotel existingHotel = getHotelById(id);

        if (existingHotel == null)
        {
            throw new RuntimeException("Hotel not found");
        }


        if (!existingHotel.getManagerId().equals(managerId))
        {
            throw new RuntimeException("You are not authorized to modify this hotel");
        }

        existingHotel.setHotelImages(updatedHotel.getHotelImages());
        existingHotel.setHotelCoverImage(updatedHotel.getHotelCoverImage());

        hotelRepository.save(existingHotel);
    }

    public void setHotelCoverImage(String hotelId, String image, String managerId)
    {
        Query query = new Query(
                Criteria.where("_id").is(hotelId)
                        .and("managerId").is(managerId)
                        .and("hotelImages").in(image)
        );

        Update update = new Update();
        update.set("hotelCoverImage", image);

        mongoTemplate.updateFirst(query, update, Hotel.class);
    }

    public void deleteHotelImage(String hotelId, String image, String managerId)
    {
        Hotel hotel = hotelRepository.findById(hotelId).orElse(null);

        if (hotel == null || !managerId.equals(hotel.getManagerId()))
        {
            return;
        }

        if (hotel.getHotelImages() == null || !hotel.getHotelImages().contains(image))
        {
            return;
        }

        hotel.getHotelImages().remove(image);

        if (image.equals(hotel.getHotelCoverImage()))
        {
            if (hotel.getHotelImages().isEmpty())
            {
                hotel.setHotelCoverImage(null);
            }
            else
            {
                hotel.setHotelCoverImage(hotel.getHotelImages().getFirst());
            }
        }

        hotelRepository.save(hotel);
        imageService.deleteHotelImageFile(image);
    }
}