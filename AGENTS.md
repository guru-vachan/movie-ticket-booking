# Development instructions

## Project prompt

Build a feature-rich movie ticket booking system using Spring Boot. Support multiple cities, theaters, shows, and seat-level booking. Customers must be able to browse shows, place time-bound seat holds, pay, confirm bookings, cancel bookings under configurable refund policies, and view booking history. Support regular, premium, weekend pricing, discount codes, and non-blocking confirmation/reminder notifications. Admins manage cities, theaters, seat layouts, shows, discounts, and refund policies.

Multiple users may attempt to book the same seat concurrently. Serialize competing requests using database transactions and row-level locking so a seat can never be allocated twice.

Use appropriate design patterns where they provide real value. Clearly expose patterns in code and documentation so architecture is easy to explain: Repository, Facade, State Machine, Strategy, Observer, and Dependency Injection. Avoid artificial pattern implementations added only for naming coverage.

## Engineering constraints

- Build a Spring Boot backend only; no UI, deployment, or advanced authentication.
- Prefer existing Spring/JDK features over added dependencies or abstractions.
- Keep seat allocation transactional and concurrency-safe.
- Validate external input and return explicit API errors.
- Test core booking flow, concurrency, and role authorization.
- Use meaningful design patterns where they improve clarity and correctness: Repository for persistence, Facade for booking orchestration, State Machine for seat lifecycle, Strategy for pricing/refunds, and Observer through transactional domain events for notifications.
- Do not add ceremonial Factory, Builder, or Adapter classes without multiple implementations or real construction/adaptation complexity.

Skills used: PDF (requirement extraction and visual verification), Ponytail (minimal implementation).
