package com.guru.movie_ticket_booking;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

interface CityRepository extends JpaRepository<City, Long> {
}

interface TheaterRepository extends JpaRepository<Theater, Long> {
}

interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByTheaterId(Long theaterId);
}

interface ShowRepository extends JpaRepository<MovieShow, Long> {
    List<MovieShow> findByStartsAtAfterOrderByStartsAt(LocalDateTime now);
}

interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {
    List<ShowSeat> findByShowIdOrderBySeatSeatLabel(Long showId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShowSeat s where s.show.id = :showId and s.id in :ids order by s.id")
    List<ShowSeat> lockSeats(@Param("showId") Long showId, @Param("ids") List<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShowSeat s where s.holdToken = :token")
    List<ShowSeat> lockByHoldToken(@Param("token") String token);
}

interface DiscountRepository extends JpaRepository<DiscountCode, String> {
}

interface RefundPolicyRepository extends JpaRepository<RefundPolicy, Long> {
    Optional<RefundPolicy> findFirstByActiveTrueOrderByCutoffMinutesDesc();
}

interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomerOrderByCreatedAtDesc(String customer);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> lockById(@Param("id") Long id);

    List<Booking> findByStatusAndReminderSentFalseAndShowStartsAtBetween(BookingStatus status, LocalDateTime from,
            LocalDateTime to);
}
