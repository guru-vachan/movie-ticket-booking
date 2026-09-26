# Movie Ticket Booking System

Spring Boot REST service for multi-city show discovery, seat holds, booking, payment simulation, cancellation, refunds, and notifications.

## Run

Requires Java 17+.

```bash
./mvnw spring-boot:run
```

Default Basic Auth users:

| User | Password | Role |
|---|---|---|
| `admin` | `admin` | Admin |
| `customer` | `customer` | Customer |
| `customer2` | `customer2` | Customer |

H2 data persists under `./data`. Console: `http://localhost:8080/h2-console`.

## API flow

Admin creates catalog:

```bash
curl -u admin:admin -H 'Content-Type: application/json' -d '{"name":"Bengaluru"}' localhost:8080/api/admin/cities
curl -u admin:admin -H 'Content-Type: application/json' -d '{"name":"PVR","cityId":1}' localhost:8080/api/admin/theaters
curl -u admin:admin -H 'Content-Type: application/json' -d '{"seats":[{"label":"A1","tier":"REGULAR"},{"label":"A2","tier":"PREMIUM"}]}' localhost:8080/api/admin/theaters/1/seats
curl -u admin:admin -H 'Content-Type: application/json' -d '{"movie":"Arrival","theaterId":1,"startsAt":"2027-01-20T20:00:00","basePrice":250}' localhost:8080/api/admin/shows
curl -u admin:admin -H 'Content-Type: application/json' -d '{"code":"SAVE10","percentOff":10}' localhost:8080/api/admin/discounts
curl -u admin:admin -H 'Content-Type: application/json' -d '{"name":"24-hour cancellation","cutoffMinutes":1440,"refundPercent":80}' localhost:8080/api/admin/refund-policies
```

Customer books:

```bash
curl -u customer:customer localhost:8080/api/shows
curl -u customer:customer localhost:8080/api/shows/1/seats
curl -u customer:customer -H 'Content-Type: application/json' -d '{"seatIds":[1,2]}' localhost:8080/api/customer/shows/1/holds
curl -u customer:customer -H 'Content-Type: application/json' -d '{"holdToken":"TOKEN_FROM_PREVIOUS_RESPONSE","paymentToken":"tok_ok","discountCode":"SAVE10"}' localhost:8080/api/customer/bookings
curl -u customer:customer localhost:8080/api/customer/bookings
curl -u customer:customer -X DELETE localhost:8080/api/customer/bookings/1
```

Run tests:

```bash
./mvnw test
```

## Design and assumptions

- Show seats are materialized from a theater layout when a show is created. Later layout changes affect future shows only.
- Holds last five minutes. Reads lazily release expired holds; locked rows can also be reclaimed immediately by another hold attempt.
- Requested seat rows use pessimistic database locks in stable ID order. Concurrent requests serialize, preventing double allocation on one database.
- Price = show base price, multiplied by 1.5 for premium seats and 1.2 for Saturday/Sunday shows, then reduced by one active discount code.
- Payment is a synchronous gateway stub: nonblank tokens succeed except tokens prefixed with `fail_`. A generated payment reference is persisted.
- One active refund policy applies globally. Cancellation before its cutoff refunds its configured percentage; later cancellation releases seats with zero refund.
- Confirmation, cancellation, and scheduled reminder delivery runs asynchronously. Logging stands in for an external email/SMS provider.
- Basic Auth and in-memory users intentionally satisfy basic RBAC only. Credentials are demo values, not production authentication.
- H2 file persistence keeps setup small. Row-lock behavior and transaction boundaries map directly to PostgreSQL for production scale.
- Catalog update/delete endpoints, payment webhooks, partial cancellation, multi-currency, seat adjacency, idempotency keys, and distributed notification delivery are excluded.

## Test coverage

Integration tests cover the full hold-confirm-cancel path, price/refund calculation, concurrent contention for one seat, authentication, and role authorization.

## Design patterns

- **Repository:** Spring Data repositories isolate persistence and database locking.
- **Facade:** `BookingService` exposes the complete hold, confirmation, history, and cancellation workflow.
- **State Machine:** show seats transition between `AVAILABLE`, `HELD`, and `BOOKED`.
- **Strategy:** `PricingStrategy` and `RefundStrategy` isolate replaceable business rules.
- **Observer:** transactional booking events notify asynchronous listeners only after a successful commit.
- **Dependency Injection:** Spring creates and wires services, repositories, and strategies.

## AI workflow

Codex read the supplied specification, scoped the domain and APIs, implemented the service using the Ponytail skill (smallest working design, platform features first), and validated behavior with Maven tests. Raw prompt artifact lives in `docs/`.


## Screenshots

![API Flow](images/booking-flow.png)

