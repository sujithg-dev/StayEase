package com.stayease.config;

import com.stayease.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingScheduler
{
    private final BookingService bookingService;

    @Scheduled(fixedRate = 3600000)
    public void updateCompletedBookings()
    {
        bookingService.updateCompletedBookings();
    }

    @Scheduled(fixedRate = 300000)
    public void cancelExpiredPayments()
    {
        bookingService.cancelExpiredPendingPayments();
    }
}