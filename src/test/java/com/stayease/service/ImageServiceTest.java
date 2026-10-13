package com.stayease.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImageServiceTest
{
    private ImageService imageService;

    @TempDir
    Path tempDir;

    private Path hotelDirectory;
    private Path roomDirectory;

    @BeforeEach
    void setUp() throws Exception
    {
        imageService = new ImageService();

        hotelDirectory = tempDir.resolve("hotels");
        roomDirectory = tempDir.resolve("rooms");

        setDirectory("hotelUploadDirectory", hotelDirectory);
        setDirectory("roomUploadDirectory", roomDirectory);
    }

    private void setDirectory(String fieldName, Path path) throws Exception
    {
        Field field = ImageService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(imageService, path);
    }

    private MockMultipartFile image(String name, String contentType, byte[] content)
    {
        return new MockMultipartFile("images", name, contentType, content);
    }

    @Test
    void saveHotelImages_whenNull_returnsEmptyList()
    {
        assertTrue(imageService.saveHotelImages(null).isEmpty());
    }

    @Test
    void saveHotelImages_savesValidImages()
    {
        MultipartFile[] images = {
                image("hotel.jpg", "image/jpeg", new byte[]{1, 2, 3}),
                image("room.png", "image/png", new byte[]{4, 5, 6})
        };

        List<String> paths = imageService.saveHotelImages(images);

        assertEquals(2, paths.size());
        assertTrue(paths.stream().allMatch(path -> path.startsWith("/uploads/hotels/")));
        assertTrue(paths.get(0).endsWith(".jpg"));
        assertTrue(paths.get(1).endsWith(".png"));
        assertEquals(2, countFiles(hotelDirectory));
    }

    @Test
    void saveHotelImages_skipsEmptyFiles()
    {
        MultipartFile[] images = {
                image("empty.jpg", "image/jpeg", new byte[0]),
                image("valid.jpg", "image/jpeg", new byte[]{1})
        };

        List<String> paths = imageService.saveHotelImages(images);

        assertEquals(1, paths.size());
        assertEquals(1, countFiles(hotelDirectory));
    }

    @Test
    void saveHotelImages_skipsNonImageFiles()
    {
        MultipartFile[] images = {
                image("document.txt", "text/plain", new byte[]{1, 2}),
                image("unknown.jpg", null, new byte[]{3, 4}),
                image("valid.jpg", "image/jpeg", new byte[]{5, 6})
        };

        List<String> paths = imageService.saveHotelImages(images);

        assertEquals(1, paths.size());
        assertEquals(1, countFiles(hotelDirectory));
    }

    @Test
    void saveHotelImages_whenFilenameHasNoExtension_savesWithoutExtension()
    {
        MultipartFile[] images = {
                image("hotelimage", "image/jpeg", new byte[]{1, 2, 3})
        };

        List<String> paths = imageService.saveHotelImages(images);

        assertEquals(1, paths.size());
        assertTrue(paths.getFirst().startsWith("/uploads/hotels/"));
        assertFalse(paths.getFirst().endsWith("."));
    }

    @Test
    void saveHRoomImages_whenNull_returnsEmptyList()
    {
        assertTrue(imageService.saveHRoomImages(null).isEmpty());
    }

    @Test

    void saveHRoomImages_savesValidImages() {
        MultipartFile[] images = {
                image("room.jpg", "image/jpeg", new byte[]{1, 2, 3}),
                image("room.png", "image/png", new byte[]{4, 5, 6})
        };

        List<String> paths = imageService.saveHRoomImages(images);

        assertEquals(2, paths.size());
        assertTrue(paths.stream().allMatch(path -> path.startsWith("/uploads/rooms/")));
        assertTrue(paths.get(0).endsWith(".jpg"));
        assertTrue(paths.get(1).endsWith(".png"));
        assertEquals(2, countFiles(roomDirectory));
    }

    @Test
    void saveHRoomImages_skipsEmptyAndNonImageFiles()
    {
        MultipartFile[] images = {
                image("empty.jpg", "image/jpeg", new byte[0]),
                image("document.pdf", "application/pdf", new byte[]{1}),
                image("valid.webp", "image/webp", new byte[]{2, 3})
        };

        List<String> paths = imageService.saveHRoomImages(images);

        assertEquals(1, paths.size());
        assertTrue(paths.getFirst().endsWith(".webp"));
        assertEquals(1, countFiles(roomDirectory));
    }

    @Test
    void deleteHotelImageFile_deletesExistingFile() throws IOException
    {
        Files.createDirectories(hotelDirectory);
        Path file = hotelDirectory.resolve("hotel.jpg");
        Files.write(file, new byte[]{1, 2, 3});

        imageService.deleteHotelImageFile("/uploads/hotels/hotel.jpg");

        assertFalse(Files.exists(file));
    }

    @Test
    void deleteHotelImageFile_whenFileDoesNotExist_doesNotThrow()
    {
        assertDoesNotThrow(() -> imageService.deleteHotelImageFile("/uploads/hotels/missing.jpg"));
    }

    @Test
    void deleteHotelImageFile_whenPathNull_doesNothing()
    {
        assertDoesNotThrow(() -> imageService.deleteHotelImageFile(null));
    }

    @Test
    void deleteHotelImageFile_whenPathBlank_doesNothing()
    {
        assertDoesNotThrow(() -> imageService.deleteHotelImageFile("  "));
    }

    @Test
    void deleteRoomImageFile_deletesExistingFile() throws IOException
    {
        Files.createDirectories(roomDirectory);
        Path file = roomDirectory.resolve("room.jpg");
        Files.write(file, new byte[]{1, 2, 3});

        imageService.deleteRoomImageFile("/uploads/rooms/room.jpg");

        assertFalse(Files.exists(file));
    }

    @Test
    void deleteRoomImageFile_whenFileDoesNotExist_doesNotThrow()
    {
        assertDoesNotThrow(() -> imageService.deleteRoomImageFile("/uploads/rooms/missing.jpg"));
    }

    @Test
    void deleteRoomImageFile_whenPathNull_doesNothing()
    {
        assertDoesNotThrow(() -> imageService.deleteRoomImageFile(null));
    }

    @Test
    void deleteRoomImageFile_whenPathBlank_doesNothing()
    {
        assertDoesNotThrow(() -> imageService.deleteRoomImageFile("  "));
    }

    @Test
    void saveHotelImages_usesUniqueFilenamesForRepeatedUploads()
    {
        MultipartFile[] firstUpload = {
                image("hotel.jpg", "image/jpeg", new byte[]{1})
        };
        MultipartFile[] secondUpload = {
                image("hotel.jpg", "image/jpeg", new byte[]{2})
        };

        String firstPath = imageService.saveHotelImages(firstUpload).getFirst();
        String secondPath = imageService.saveHotelImages(secondUpload).getFirst();

        assertNotEquals(firstPath, secondPath);
        assertEquals(2, countFiles(hotelDirectory));
    }

    private long countFiles(Path directory)
    {
        if (!Files.exists(directory))
        {
            return 0;
        }

        try (var files = Files.list(directory))
        {
            return files.count();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}