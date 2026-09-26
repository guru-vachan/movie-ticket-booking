package com.guru.movie_ticket_booking;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

interface PricingStrategy {
    BigDecimal calculate(MovieShow show, List<ShowSeat> seats, DiscountCode discount);
}

@Component
class TieredPricingStrategy implements PricingStrategy {
    @Override
    public BigDecimal calculate(MovieShow show, List<ShowSeat> seats, DiscountCode discount) {
        BigDecimal total = seats.stream().map(s -> s.seat.tier == SeatTier.PREMIUM
                ? show.basePrice.multiply(BigDecimal.valueOf(1.5))
                : show.basePrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        DayOfWeek day = show.startsAt.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY)
            total = total.multiply(BigDecimal.valueOf(1.2));
        if (discount != null)
            total = total.multiply(BigDecimal.valueOf(100 - discount.percentOff)).divide(BigDecimal.valueOf(100));
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}

interface RefundStrategy {
    BigDecimal calculate(Booking booking, RefundPolicy policy, LocalDateTime now);
}

@Component
class PolicyBasedRefundStrategy implements RefundStrategy {
    @Override
    public BigDecimal calculate(Booking booking, RefundPolicy policy, LocalDateTime now) {
        long minutes = Duration.between(now, booking.show.startsAt).toMinutes();
        int percent = minutes >= policy.cutoffMinutes ? policy.refundPercent : 0;
        return booking.amountPaid.multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
