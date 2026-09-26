package com.guru.movie_ticket_booking;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
class BookingJobs {
    private final BookingRepository bookings;
    private final NotificationService notifications;
    private final long reminderMinutes;

    BookingJobs(BookingRepository bookings, NotificationService notifications,
            @Value("${booking.reminder-minutes:60}") long reminderMinutes) {
        this.bookings = bookings;
        this.notifications = notifications;
        this.reminderMinutes = reminderMinutes;
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    void sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        bookings.findByStatusAndReminderSentFalseAndShowStartsAtBetween(BookingStatus.CONFIRMED, now,
                now.plusMinutes(reminderMinutes))
                .forEach(b -> {
                    b.reminderSent = true;
                    notifications.reminder(b.id, b.customer);
                });
    }
}
