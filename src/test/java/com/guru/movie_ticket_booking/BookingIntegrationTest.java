package com.guru.movie_ticket_booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = { "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop" })
class BookingIntegrationTest {
    @Autowired
    BookingService service;
    @Autowired
    CityRepository cities;
    @Autowired
    TheaterRepository theaters;
    @Autowired
    SeatRepository seats;
    @Autowired
    ShowRepository shows;
    @Autowired
    ShowSeatRepository showSeats;
    @Autowired
    RefundPolicyRepository policies;
    MovieShow show;
    ShowSeat showSeat;
    Principal customer = new UsernamePasswordAuthenticationToken("customer", "n/a");

    @BeforeEach
    void setUp() {
        showSeats.deleteAll();
        shows.deleteAll();
        seats.deleteAll();
        theaters.deleteAll();
        cities.deleteAll();
        policies.deleteAll();
        City city = cities.save(new City("Bengaluru"));
        Theater theater = theaters.save(new Theater("Central", city));
        Seat seat = seats.save(new Seat(theater, "A1", SeatTier.PREMIUM));
        show = shows.save(new MovieShow("Arrival", theater, LocalDateTime.now().plusDays(2), BigDecimal.valueOf(200)));
        showSeat = showSeats.save(new ShowSeat(show, seat));
        policies.save(new RefundPolicy("24 hour", 1440, 80));
    }

    @Test
    void holdsConfirmsAndCancelsWithRefund() {
        HoldResponse hold = service.hold(show.id, List.of(showSeat.id), customer);
        assertThat(hold.amount()).isEqualByComparingTo("300.00");
        BookingResponse booking = service.confirm(new ConfirmRequest(hold.holdToken(), "tok_ok", null), customer);
        assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);
        BookingResponse cancelled = service.cancel(booking.id(), customer);
        assertThat(cancelled.refundAmount()).isEqualByComparingTo("240.00");
        assertThat(service.seats(show.id).get(0).status()).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    void concurrentHoldsAllocateSeatOnce() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> attempt = () -> {
            start.await();
            try {
                service.hold(show.id, List.of(showSeat.id), customer);
                return true;
            } catch (ApiException e) {
                return false;
            }
        };
        Future<Boolean> first = pool.submit(attempt), second = pool.submit(attempt);
        start.countDown();
        assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        pool.shutdownNow();
    }
}
