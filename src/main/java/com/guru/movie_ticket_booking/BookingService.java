package com.guru.movie_ticket_booking;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.*;
import java.util.List;
import java.util.UUID;

@Service
class BookingService {
    private final ShowRepository shows;
    private final ShowSeatRepository showSeats;
    private final DiscountRepository discounts;
    private final RefundPolicyRepository policies;
    private final BookingRepository bookings;
    private final PricingStrategy pricing;
    private final RefundStrategy refunds;
    private final BookingEventPublisher events;
    private final long holdMinutes;

    BookingService(ShowRepository shows, ShowSeatRepository showSeats, DiscountRepository discounts,
            RefundPolicyRepository policies, BookingRepository bookings, PricingStrategy pricing,
            RefundStrategy refunds, BookingEventPublisher events,
            @Value("${booking.hold-minutes:5}") long holdMinutes) {
        this.shows = shows;
        this.showSeats = showSeats;
        this.discounts = discounts;
        this.policies = policies;
        this.bookings = bookings;
        this.pricing = pricing;
        this.refunds = refunds;
        this.events = events;
        this.holdMinutes = holdMinutes;
    }

    @Transactional
    HoldResponse hold(Long showId, List<Long> requestedIds, Principal principal) {
        if (requestedIds.isEmpty() || requestedIds.size() != requestedIds.stream().distinct().count())
            throw ApiException.badRequest("Seat IDs must be non-empty and unique");
        MovieShow show = shows.findById(showId).orElseThrow(() -> ApiException.notFound("Show"));
        if (!show.startsAt.isAfter(LocalDateTime.now()))
            throw ApiException.badRequest("Show has already started");
        List<Long> ids = requestedIds.stream().sorted().toList();
        List<ShowSeat> seats = showSeats.lockSeats(showId, ids);
        if (seats.size() != ids.size())
            throw ApiException.badRequest("One or more seats do not belong to this show");
        Instant now = Instant.now();
        seats.forEach(this::releaseIfExpired);
        if (seats.stream().anyMatch(s -> s.status != SeatStatus.AVAILABLE))
            throw ApiException.conflict("One or more seats are unavailable");
        String token = UUID.randomUUID().toString();
        Instant expiresAt = now.plus(Duration.ofMinutes(holdMinutes));
        seats.forEach(s -> {
            s.status = SeatStatus.HELD;
            s.holdToken = token;
            s.customer = principal.getName();
            s.holdExpiresAt = expiresAt;
        });
        return new HoldResponse(token, expiresAt, pricing.calculate(show, seats, null),
                seats.stream().map(s -> s.id).toList());
    }

    @Transactional
    BookingResponse confirm(ConfirmRequest request, Principal principal) {
        List<ShowSeat> seats = showSeats.lockByHoldToken(request.holdToken());
        if (seats.isEmpty())
            throw ApiException.notFound("Hold");
        Instant now = Instant.now();
        if (seats.stream().anyMatch(s -> s.status != SeatStatus.HELD || now.isAfter(s.holdExpiresAt))) {
            seats.forEach(this::releaseIfExpired);
            throw new ApiException(HttpStatus.GONE, "Hold expired");
        }
        if (seats.stream().anyMatch(s -> !principal.getName().equals(s.customer)))
            throw new ApiException(HttpStatus.FORBIDDEN, "Hold belongs to another customer");
        if (request.paymentToken().isBlank() || request.paymentToken().startsWith("fail_"))
            throw ApiException.badRequest("Payment declined");
        MovieShow show = seats.get(0).show;
        DiscountCode discount = request.discountCode() == null ? null
                : discounts.findById(request.discountCode().toUpperCase())
                        .filter(d -> d.active).orElseThrow(() -> ApiException.badRequest("Invalid discount code"));
        BigDecimal amount = pricing.calculate(show, seats, discount);
        seats.forEach(s -> {
            s.status = SeatStatus.BOOKED;
            s.holdToken = null;
            s.holdExpiresAt = null;
        });
        Booking booking = bookings
                .save(new Booking(principal.getName(), show, seats, amount, "pay_" + UUID.randomUUID()));
        events.confirmed(booking);
        return BookingResponse.from(booking);
    }

    @Transactional
    BookingResponse cancel(Long id, Principal principal) {
        Booking booking = bookings.lockById(id).orElseThrow(() -> ApiException.notFound("Booking"));
        if (!booking.customer.equals(principal.getName()))
            throw new ApiException(HttpStatus.FORBIDDEN, "Booking belongs to another customer");
        if (booking.status == BookingStatus.CANCELLED)
            throw ApiException.conflict("Booking already cancelled");
        if (!booking.show.startsAt.isAfter(LocalDateTime.now()))
            throw ApiException.badRequest("Show has already started");
        RefundPolicy policy = policies.findFirstByActiveTrueOrderByCutoffMinutesDesc()
                .orElse(new RefundPolicy("default", 0, 0));
        booking.refundAmount = refunds.calculate(booking, policy, LocalDateTime.now());
        booking.status = BookingStatus.CANCELLED;
        booking.cancelledAt = Instant.now();
        booking.seats.forEach(s -> {
            s.status = SeatStatus.AVAILABLE;
            s.customer = null;
        });
        events.cancelled(booking);
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    List<BookingResponse> history(Principal principal) {
        return bookings.findByCustomerOrderByCreatedAtDesc(principal.getName()).stream().map(BookingResponse::from)
                .toList();
    }

    @Transactional
    List<SeatResponse> seats(Long showId) {
        if (!shows.existsById(showId))
            throw ApiException.notFound("Show");
        return showSeats.findByShowIdOrderBySeatSeatLabel(showId).stream().map(s -> {
            releaseIfExpired(s);
            return SeatResponse.from(s);
        }).toList();
    }

    private void releaseIfExpired(ShowSeat seat) {
        if (seat.status == SeatStatus.HELD && seat.holdExpiresAt != null
                && !seat.holdExpiresAt.isAfter(Instant.now())) {
            seat.status = SeatStatus.AVAILABLE;
            seat.holdToken = null;
            seat.customer = null;
            seat.holdExpiresAt = null;
        }
    }

}

record HoldResponse(String holdToken, Instant expiresAt, BigDecimal amount, List<Long> seatIds) {
}

record ConfirmRequest(String holdToken, String paymentToken, String discountCode) {
}

record SeatResponse(Long id, String label, SeatTier tier, SeatStatus status, Instant heldUntil) {
    static SeatResponse from(ShowSeat s) {
        return new SeatResponse(s.id, s.seat.seatLabel, s.seat.tier, s.status, s.holdExpiresAt);
    }
}

record BookingResponse(Long id, Long showId, String movie, List<Long> seatIds, BigDecimal amountPaid,
        BookingStatus status, BigDecimal refundAmount, Instant createdAt) {
    static BookingResponse from(Booking b) {
        return new BookingResponse(b.id, b.show.id, b.show.movie,
                b.seats.stream().map(s -> s.id).toList(), b.amountPaid, b.status, b.refundAmount, b.createdAt);
    }
}
