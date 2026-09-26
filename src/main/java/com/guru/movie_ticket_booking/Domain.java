package com.guru.movie_ticket_booking;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

enum SeatTier {
    REGULAR, PREMIUM
}

enum SeatStatus {
    AVAILABLE, HELD, BOOKED
}

enum BookingStatus {
    CONFIRMED, CANCELLED
}

@Entity
class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, unique = true)
    String name;

    protected City() {
    }

    City(String name) {
        this.name = name;
    }
}

@Entity
class Theater {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false)
    String name;
    @ManyToOne(optional = false)
    City city;

    protected Theater() {
    }

    Theater(String name, City city) {
        this.name = name;
        this.city = city;
    }
}

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "theater_id", "seatLabel" }))
class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne(optional = false)
    Theater theater;
    @Column(nullable = false)
    String seatLabel;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    SeatTier tier;

    protected Seat() {
    }

    Seat(Theater theater, String label, SeatTier tier) {
        this.theater = theater;
        this.seatLabel = label;
        this.tier = tier;
    }
}

@Entity
class MovieShow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false)
    String movie;
    @ManyToOne(optional = false)
    Theater theater;
    @Column(nullable = false)
    LocalDateTime startsAt;
    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal basePrice;

    protected MovieShow() {
    }

    MovieShow(String movie, Theater theater, LocalDateTime startsAt, BigDecimal basePrice) {
        this.movie = movie;
        this.theater = theater;
        this.startsAt = startsAt;
        this.basePrice = basePrice;
    }
}

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "show_id", "seat_id" }))
class ShowSeat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "show_id")
    MovieShow show;
    @ManyToOne(optional = false)
    @JoinColumn(name = "seat_id")
    Seat seat;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    SeatStatus status = SeatStatus.AVAILABLE;
    String holdToken;
    String customer;
    Instant holdExpiresAt;

    protected ShowSeat() {
    }

    ShowSeat(MovieShow show, Seat seat) {
        this.show = show;
        this.seat = seat;
    }
}

@Entity
class DiscountCode {
    @Id
    String code;
    @Column(nullable = false)
    int percentOff;
    @Column(nullable = false)
    boolean active = true;

    protected DiscountCode() {
    }

    DiscountCode(String code, int percentOff) {
        this.code = code;
        this.percentOff = percentOff;
    }
}

@Entity
class RefundPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false)
    String name;
    @Column(nullable = false)
    long cutoffMinutes;
    @Column(nullable = false)
    int refundPercent;
    @Column(nullable = false)
    boolean active = true;

    protected RefundPolicy() {
    }

    RefundPolicy(String name, long cutoffMinutes, int refundPercent) {
        this.name = name;
        this.cutoffMinutes = cutoffMinutes;
        this.refundPercent = refundPercent;
    }
}

@Entity
class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false)
    String customer;
    @ManyToOne(optional = false)
    MovieShow show;
    @OneToMany(fetch = FetchType.EAGER)
    List<ShowSeat> seats = new ArrayList<>();
    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal amountPaid;
    @Column(nullable = false)
    String paymentReference;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    BookingStatus status = BookingStatus.CONFIRMED;
    @Column(nullable = false)
    Instant createdAt = Instant.now();
    BigDecimal refundAmount;
    Instant cancelledAt;
    boolean reminderSent;

    protected Booking() {
    }

    Booking(String customer, MovieShow show, List<ShowSeat> seats, BigDecimal amount, String paymentReference) {
        this.customer = customer;
        this.show = show;
        this.seats.addAll(seats);
        this.amountPaid = amount;
        this.paymentReference = paymentReference;
    }
}
