package com.stayease.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest
{
    private static final String CLOUD_URL = "https://res.cloudinary.com/demo/image/upload/v123/";

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @Captor
    private ArgumentCaptor<Map<String, Object>> optionsCaptor;

    private ImageService imageService;

    @BeforeEach
    void setUp()
    {
        imageService = new ImageService(cloudinary);
        lenient().when(cloudinary.uploader()).thenReturn(uploader);
    }

    private void stubUploadSuccess() throws IOException
    {
        when(uploader.upload(any(), anyMap())).thenAnswer(invocation ->
        {
            Map<?, ?> options = invocation.getArgument(1);
            return Map.of("secure_url",
                    CLOUD_URL + options.get("folder") + "/" + options.get("public_id") + ".jpg");
        });
    }

    private MockMultipartFile image(String name, String contentType, byte[] content)
    {
        return new MockMultipartFile("images", name, contentType, content);
    }

    @Test
    void saveHotelImages_whenNull_returnsEmptyList()
    {
        assertTrue(imageService.saveHotelImages(null).isEmpty());
        verifyNoInteractions(uploader);
    }

    @Test
    void saveHotelImages_uploadsValidImagesToHotelFolder() throws Exception
    {
        stubUploadSuccess();

        MultipartFile[] images = {
                image("hotel.jpg", "image/jpeg", new byte[]{1, 2, 3}),
                image("room.png", "image/png", new byte[]{4, 5, 6})
        };

        List<String> urls = imageService.saveHotelImages(images);

        assertEquals(2, urls.size());
        assertTrue(urls.stream().allMatch(url -> url.startsWith(CLOUD_URL + "stayease/hotels/")));

        verify(uploader, times(2)).upload(any(), optionsCaptor.capture());
        assertTrue(optionsCaptor.getAllValues().stream()
                .allMatch(o -> "stayease/hotels".equals(o.get("folder"))));
    }

    @Test
    void saveHotelImages_skipsEmptyFiles() throws Exception
    {
        stubUploadSuccess();

        MultipartFile[] images = {
                image("empty.jpg", "image/jpeg", new byte[0]),
                image("valid.jpg", "image/jpeg", new byte[]{1})
        };

        List<String> urls = imageService.saveHotelImages(images);

        assertEquals(1, urls.size());
        verify(uploader, times(1)).upload(any(), anyMap());
    }

    @Test
    void saveHotelImages_skipsNonImageFiles() throws Exception
    {
        stubUploadSuccess();

        MultipartFile[] images = {
                image("document.txt", "text/plain", new byte[]{1, 2}),
                image("unknown.jpg", null, new byte[]{3, 4}),
                image("valid.jpg", "image/jpeg", new byte[]{5, 6})
        };

        List<String> urls = imageService.saveHotelImages(images);

        assertEquals(1, urls.size());
        verify(uploader, times(1)).upload(any(), anyMap());
    }

    @Test
    void saveHotelImages_usesUniquePublicIdsForRepeatedUploads() throws Exception
    {
        stubUploadSuccess();

        MultipartFile[] images = {
                image("hotel.jpg", "image/jpeg", new byte[]{1}),
                image("hotel.jpg", "image/jpeg", new byte[]{2})
        };

        List<String> urls = imageService.saveHotelImages(images);

        assertEquals(2, urls.size());
        assertEquals(2, new HashSet<>(urls).size());
    }

    @Test
    void saveHotelImages_whenUploadFails_throwsRuntimeException() throws Exception
    {
        when(uploader.upload(any(), anyMap())).thenThrow(new IOException("network down"));

        MultipartFile[] images = { image("hotel.jpg", "image/jpeg", new byte[]{1}) };

        assertThrows(RuntimeException.class, () -> imageService.saveHotelImages(images));
    }

    @Test
    void saveHRoomImages_whenNull_returnsEmptyList()
    {
        assertTrue(imageService.saveHRoomImages(null).isEmpty());
        verifyNoInteractions(uploader);
    }

    @Test
    void saveHRoomImages_uploadsValidImagesToRoomFolder() throws Exception
    {
        stubUploadSuccess();

        MultipartFile[] images = {
                image("room.jpg", "image/jpeg", new byte[]{1, 2, 3}),
                image("room.png", "image/png", new byte[]{4, 5, 6})
        };

        List<String> urls = imageService.saveHRoomImages(images);

        assertEquals(2, urls.size());
        assertTrue(urls.stream().allMatch(url -> url.startsWith(CLOUD_URL + "stayease/rooms/")));

        verify(uploader, times(2)).upload(any(), optionsCaptor.capture());
        assertTrue(optionsCaptor.getAllValues().stream()
                .allMatch(o -> "stayease/rooms".equals(o.get("folder"))));
    }

    @Test
    void saveHRoomImages_skipsEmptyAndNonImageFiles() throws Exception
    {
        stubUploadSuccess();

        MultipartFile[] images = {
                image("empty.jpg", "image/jpeg", new byte[0]),
                image("document.pdf", "application/pdf", new byte[]{1}),
                image("valid.webp", "image/webp", new byte[]{2, 3})
        };

        List<String> urls = imageService.saveHRoomImages(images);

        assertEquals(1, urls.size());
        verify(uploader, times(1)).upload(any(), anyMap());
    }

    // ---------- delete ----------

    @Test
    void deleteHotelImageFile_callsDestroyWithPublicId() throws Exception
    {
        imageService.deleteHotelImageFile(CLOUD_URL + "stayease/hotels/abc123.jpg");

        verify(uploader).destroy(eq("stayease/hotels/abc123"), anyMap());
    }

    @Test
    void deleteRoomImageFile_callsDestroyWithPublicId() throws Exception
    {
        imageService.deleteRoomImageFile(CLOUD_URL + "stayease/rooms/xyz789.png");

        verify(uploader).destroy(eq("stayease/rooms/xyz789"), anyMap());
    }

    @Test
    void deleteHotelImageFile_whenUrlHasNoVersion_stillExtractsPublicId() throws Exception
    {
        imageService.deleteHotelImageFile(
                "https://res.cloudinary.com/demo/image/upload/stayease/hotels/abc123.jpg");

        verify(uploader).destroy(eq("stayease/hotels/abc123"), anyMap());
    }

    @Test
    void deleteHotelImageFile_ignoresOldLocalPaths() throws Exception
    {
        imageService.deleteHotelImageFile("/uploads/hotels/old-image.jpg");

        verifyNoInteractions(uploader);
    }

    @Test
    void deleteRoomImageFile_ignoresOldLocalPaths() throws Exception
    {
        imageService.deleteRoomImageFile("/uploads/rooms/old-image.jpg");

        verifyNoInteractions(uploader);
    }

    @Test
    void deleteHotelImageFile_whenPathNullOrBlank_doesNothing()
    {
        assertDoesNotThrow(() -> imageService.deleteHotelImageFile(null));
        assertDoesNotThrow(() -> imageService.deleteHotelImageFile("  "));
        verifyNoInteractions(uploader);
    }

    @Test
    void deleteRoomImageFile_whenPathNullOrBlank_doesNothing()
    {
        assertDoesNotThrow(() -> imageService.deleteRoomImageFile(null));
        assertDoesNotThrow(() -> imageService.deleteRoomImageFile("  "));
        verifyNoInteractions(uploader);
    }

    @Test
    void deleteHotelImageFile_whenDestroyFails_throwsRuntimeException() throws Exception
    {
        when(uploader.destroy(anyString(), anyMap())).thenThrow(new IOException("fail"));

        assertThrows(RuntimeException.class, () ->
                imageService.deleteHotelImageFile(CLOUD_URL + "stayease/hotels/abc123.jpg"));
    }
}