package com.guru.movie_ticket_booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
class CustomerController {
    private final BookingService service;

    CustomerController(BookingService service) {
        this.service = service;
    }

    @GetMapping("/api/shows/{showId}/seats")
    List<SeatResponse> seats(@PathVariable Long showId) {
        return service.seats(showId);
    }

    @PostMapping("/api/customer/shows/{showId}/holds")
    HoldResponse hold(@PathVariable Long showId, @Valid @RequestBody HoldRequest request, Principal principal) {
        return service.hold(showId, request.seatIds(), principal);
    }

    @PostMapping("/api/customer/bookings")
    BookingResponse confirm(@Valid @RequestBody ConfirmBookingRequest request, Principal principal) {
        return service.confirm(new ConfirmRequest(request.holdToken(), request.paymentToken(), request.discountCode()),
                principal);
    }

    @GetMapping("/api/customer/bookings")
    List<BookingResponse> history(Principal principal) {
        return service.history(principal);
    }

    @DeleteMapping("/api/customer/bookings/{id}")
    BookingResponse cancel(@PathVariable Long id, Principal principal) {
        return service.cancel(id, principal);
    }
}

record HoldRequest(@NotEmpty List<Long> seatIds) {
}

record ConfirmBookingRequest(@NotBlank String holdToken, @NotBlank String paymentToken, String discountCode) {
}
