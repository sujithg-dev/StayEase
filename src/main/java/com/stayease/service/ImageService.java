package com.stayease.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService
{
    private static final String HOTEL_FOLDER = "stayease/hotels";
    private static final String ROOM_FOLDER = "stayease/rooms";

    private final Cloudinary cloudinary;

    public List<String> saveHotelImages(MultipartFile[] images)
    {
        return uploadImages(images, HOTEL_FOLDER);
    }

    public List<String> saveHRoomImages(MultipartFile[] images)
    {
        return uploadImages(images, ROOM_FOLDER);
    }

    public void deleteHotelImageFile(String imageUrl)
    {
        deleteImage(imageUrl);
    }

    public void deleteRoomImageFile(String imageUrl)
    {
        deleteImage(imageUrl);
    }

    private List<String> uploadImages(MultipartFile[] images, String folder)
    {
        List<String> imageUrls = new ArrayList<>();

        if (images == null)
        {
            return imageUrls;
        }

        try
        {
            for (MultipartFile image : images)
            {
                if (image.isEmpty())
                {
                    continue;
                }

                String contentType = image.getContentType();

                if (contentType == null || !contentType.startsWith("image/"))
                {
                    continue;
                }

                Map<?, ?> result = cloudinary.uploader().upload(
                        image.getBytes(),
                        ObjectUtils.asMap(
                                "folder", folder,
                                "public_id", UUID.randomUUID().toString(),
                                "resource_type", "image"));

                imageUrls.add(result.get("secure_url").toString());
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to upload images", e);
        }

        return imageUrls;
    }

    private void deleteImage(String imageUrl)
    {
        if (imageUrl == null || imageUrl.isBlank())
        {
            return;
        }

        if (!imageUrl.contains("res.cloudinary.com"))
        {
            return;
        }

        try
        {
            cloudinary.uploader().destroy(extractPublicId(imageUrl), ObjectUtils.asMap("invalidate", true));
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to delete image", e);
        }
    }

    private String extractPublicId(String url)
    {
        String afterUpload = url.substring(url.indexOf("/upload/") + "/upload/".length());

        if (afterUpload.matches("^v\\d+/.*"))
        {
            afterUpload = afterUpload.substring(afterUpload.indexOf('/') + 1);
        }

        int dot = afterUpload.lastIndexOf('.');
        return dot > 0 ? afterUpload.substring(0, dot) : afterUpload;
    }
}