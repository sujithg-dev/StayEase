package com.stayease.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService
{
    private final Path hotelUploadDirectory = Paths.get("uploads/hotels");
    private final Path roomUploadDirectory = Paths.get("uploads/rooms");

    public List<String> saveHotelImages(MultipartFile[] images) {List<String> imagePaths = new ArrayList<>();

        if (images == null)
        {
            return imagePaths;
        }

        try
        {
            Files.createDirectories(hotelUploadDirectory);

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

                String extension = getExtension(image.getOriginalFilename());
                String fileName = UUID.randomUUID() + extension;
                Path filePath = hotelUploadDirectory.resolve(fileName);

                Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                imagePaths.add("/uploads/hotels/" + fileName);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to save hotel images", e);
        }

        return imagePaths;
    }

    public List<String> saveHRoomImages(MultipartFile[] images) {List<String> imagePaths = new ArrayList<>();

        if (images == null) return imagePaths;

        try {
            Files.createDirectories(roomUploadDirectory);

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

                String extension = getExtension(image.getOriginalFilename());
                String fileName = UUID.randomUUID() + extension;
                Path filePath = roomUploadDirectory.resolve(fileName);

                Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                imagePaths.add("/uploads/rooms/" + fileName);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to save hotel images", e);
        }

        return imagePaths;
    }

    public void deleteHotelImageFile(String imagePath)
    {
        if (imagePath == null || imagePath.isBlank())
        {
            return;
        }

        try
        {
            String fileName = imagePath.substring(imagePath.lastIndexOf("/") + 1);
            Path filePath = hotelUploadDirectory.resolve(fileName);
            Files.deleteIfExists(filePath);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to delete hotel image", e);
        }
    }

    public void deleteRoomImageFile(String imagePath)
    {
        if (imagePath == null || imagePath.isBlank())
        {
            return;
        }

        try
        {
            String fileName = imagePath.substring(imagePath.lastIndexOf("/") + 1);
            Path filePath = roomUploadDirectory.resolve(fileName);
            Files.deleteIfExists(filePath);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to delete room image", e);
        }
    }

    private String getExtension(String fileName)
    {
        if (fileName == null || !fileName.contains("."))
        {
            return "";
        }

        return fileName.substring(fileName.lastIndexOf("."));
    }
}