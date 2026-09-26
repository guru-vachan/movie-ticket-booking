package com.guru.movie_ticket_booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
class AdminController {
    private final CityRepository cities;
    private final TheaterRepository theaters;
    private final SeatRepository seats;
    private final ShowRepository shows;
    private final ShowSeatRepository showSeats;
    private final DiscountRepository discounts;
    private final RefundPolicyRepository policies;

    AdminController(CityRepository cities, TheaterRepository theaters, SeatRepository seats, ShowRepository shows,
            ShowSeatRepository showSeats, DiscountRepository discounts, RefundPolicyRepository policies) {
        this.cities = cities;
        this.theaters = theaters;
        this.seats = seats;
        this.shows = shows;
        this.showSeats = showSeats;
        this.discounts = discounts;
        this.policies = policies;
    }

    @PostMapping("/cities")
    @ResponseStatus(HttpStatus.CREATED)
    CityView city(@Valid @RequestBody CityRequest r) {
        City c = cities.save(new City(r.name()));
        return new CityView(c.id, c.name);
    }

    @PostMapping("/theaters")
    @ResponseStatus(HttpStatus.CREATED)
    TheaterView theater(@Valid @RequestBody TheaterRequest r) {
        City city = cities.findById(r.cityId()).orElseThrow(() -> ApiException.notFound("City"));
        Theater t = theaters.save(new Theater(r.name(), city));
        return new TheaterView(t.id, t.name, city.id);
    }

    @PostMapping("/theaters/{theaterId}/seats")
    @ResponseStatus(HttpStatus.CREATED)
    List<SeatView> layout(@PathVariable Long theaterId, @Valid @RequestBody SeatLayoutRequest r) {
        Theater theater = theaters.findById(theaterId).orElseThrow(() -> ApiException.notFound("Theater"));
        return seats.saveAll(r.seats().stream().map(s -> new Seat(theater, s.label(), s.tier())).toList()).stream()
                .map(s -> new SeatView(s.id, s.seatLabel, s.tier)).toList();
    }

    @PostMapping("/shows")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    ShowView show(@Valid @RequestBody ShowRequest r) {
        Theater theater = theaters.findById(r.theaterId()).orElseThrow(() -> ApiException.notFound("Theater"));
        if (!r.startsAt().isAfter(LocalDateTime.now()))
            throw ApiException.badRequest("startsAt must be in the future");
        List<Seat> layout = seats.findByTheaterId(theater.id);
        if (layout.isEmpty())
            throw ApiException.badRequest("Theater has no seat layout");
        MovieShow show = shows.save(new MovieShow(r.movie(), theater, r.startsAt(), r.basePrice()));
        showSeats.saveAll(layout.stream().map(s -> new ShowSeat(show, s)).toList());
        return ShowView.from(show);
    }

    @PostMapping("/discounts")
    @ResponseStatus(HttpStatus.CREATED)
    DiscountView discount(@Valid @RequestBody DiscountRequest r) {
        DiscountCode d = discounts.save(new DiscountCode(r.code().toUpperCase(), r.percentOff()));
        return new DiscountView(d.code, d.percentOff, d.active);
    }

    @PostMapping("/refund-policies")
    @ResponseStatus(HttpStatus.CREATED)
    RefundPolicyView policy(@Valid @RequestBody RefundPolicyRequest r) {
        RefundPolicy p = policies.save(new RefundPolicy(r.name(), r.cutoffMinutes(), r.refundPercent()));
        return new RefundPolicyView(p.id, p.name, p.cutoffMinutes, p.refundPercent, p.active);
    }
}

@RestController
class ShowController {
    private final ShowRepository shows;

    ShowController(ShowRepository shows) {
        this.shows = shows;
    }

    @GetMapping("/api/shows")
    List<ShowView> shows() {
        return shows.findByStartsAtAfterOrderByStartsAt(LocalDateTime.now()).stream().map(ShowView::from).toList();
    }
}

record CityRequest(@NotBlank String name) {
}

record TheaterRequest(@NotBlank String name, @NotNull Long cityId) {
}

record SeatLayoutRequest(@NotEmpty List<@Valid SeatRequest> seats) {
}

record SeatRequest(@NotBlank String label, @NotNull SeatTier tier) {
}

record ShowRequest(@NotBlank String movie, @NotNull Long theaterId, @NotNull LocalDateTime startsAt,
        @NotNull @DecimalMin("0.01") BigDecimal basePrice) {
}

record DiscountRequest(@NotBlank String code, @Min(1) @Max(100) int percentOff) {
}

record RefundPolicyRequest(@NotBlank String name, @PositiveOrZero long cutoffMinutes,
        @Min(0) @Max(100) int refundPercent) {
}

record CityView(Long id, String name) {
}

record TheaterView(Long id, String name, Long cityId) {
}

record SeatView(Long id, String label, SeatTier tier) {
}

record DiscountView(String code, int percentOff, boolean active) {
}

record RefundPolicyView(Long id, String name, long cutoffMinutes, int refundPercent, boolean active) {
}

record ShowView(Long id, String movie, Long theaterId, String theater, String city, LocalDateTime startsAt,
        BigDecimal basePrice) {
    static ShowView from(MovieShow s) {
        return new ShowView(s.id, s.movie, s.theater.id, s.theater.name, s.theater.city.name, s.startsAt, s.basePrice);
    }
}
