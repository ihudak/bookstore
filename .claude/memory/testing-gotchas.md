# Testing gotchas (repo-specific)

### Shared in-memory H2 across Spring test contexts
- **Context**: Ratings tests (RatingControllerIT and friends) use several Spring contexts that all point at `jdbc:h2:mem:db` with `ddl-auto=create-drop`.
- **Insight**: The contexts share one database, so `@DirtiesContext` and `@Transactional` do not give isolation and fight the shared schema (and `@Transactional` would hide unique-constraint and commit behaviour). Instead, empty the table in `@BeforeEach` of every test class. For MySQL-flavoured SQL and case-insensitive e-mail matching use `MODE=MySQL;IGNORECASE=TRUE` in the H2 test URL.
- **When it applies**: Adding or changing integration tests in any service that uses the shared H2 test URL, especially tests for unique constraints or concurrency (which need real commits).
- **Date**: 2026-10-13
- **Ref**: Implement BOOK-2-01-01 buyers-only ratings in bookstore:rat

### MockRestServiceServer: register all expectations before the first request
- **Context**: A ratings test needing several answers from the orders service added expectations after a request had already been made.
- **Insight**: `MockRestServiceServer` refuses new expectations once a request has been executed. Register every expected call up front (in order), or use a hand-written in-memory port implementation (e.g. `InMemoryPurchaseCheck`) for tests that do not exercise the HTTP client itself.
- **When it applies**: Any test of a RestTemplate-based inter-service client that makes more than one call in a scenario.
- **Date**: 2026-10-13
- **Ref**: Implement BOOK-2-01-01 buyers-only ratings in bookstore:rat

### Assert JSON keys, not bare substrings, in "field absent" checks
- **Context**: A "no comment in the answer" test failed because the response contained `commenter@example.com`.
- **Insight**: When asserting a field is absent from a response body, match the quoted JSON key (`"\"comment\""`) or use a JSON path check; a bare word will match test data such as e-mail addresses.
- **When it applies**: Tests asserting that `@JsonIgnore`d or removed fields are not serialized.
- **Date**: 2026-10-13
- **Ref**: Implement BOOK-2-01-01 buyers-only ratings in bookstore:rat
