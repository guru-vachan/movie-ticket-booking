package com.guru.movie_ticket_booking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

@Service
class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void confirmation(BookingConfirmed event) {
        log.info("Confirmation sent: booking={}, customer={}", event.bookingId(), event.customer());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void cancellation(BookingCancelled event) {
        log.info("Cancellation sent: booking={}, customer={}, refund={}", event.bookingId(), event.customer(),
                event.refund());
    }

    @Async
    void reminder(Long bookingId, String customer) {
        log.info("Reminder sent: booking={}, customer={}", bookingId, customer);
    }
}
