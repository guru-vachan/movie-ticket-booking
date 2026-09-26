package com.guru.movie_ticket_booking;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

record BookingConfirmed(Long bookingId, String customer) {
}

record BookingCancelled(Long bookingId, String customer, BigDecimal refund) {
}

@Component
class BookingEventPublisher {
    private final ApplicationEventPublisher publisher;

    BookingEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    void confirmed(Booking booking) {
        publisher.publishEvent(new BookingConfirmed(booking.id, booking.customer));
    }

    void cancelled(Booking booking) {
        publisher.publishEvent(new BookingCancelled(booking.id, booking.customer, booking.refundAmount));
    }
}
