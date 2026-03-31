# BookStore Query Rules

## Service Filtering
When querying BookStore services on Dynatrace (Managed or SaaS), always exclude:
- "apponly" services (e.g. BookStore.Carts.apponly, BookStore.Payments.apponly, etc.)
- "SpringBoot ..." prefixed services
- OTel-instrumented service variants

Only use the core named BookStore services:
- BookStore-Books
- BookStore-Orders
- BookStore-Carts
- BookStore-Storage
- BookStore-Ratings
- BookStore-Payments
- BookStore-Dynapay
- BookStore-Ingest
- BookStore-Clients

### Known Managed Prod entity IDs (core services only)
- SERVICE-05CE68CA64BD7B5B — BookStore-Books
- SERVICE-C3AB15ED472C8B74 — BookStore-Orders
- SERVICE-2CF31E2B554B1524 — BookStore-Ingest
- SERVICE-B7555D1B5EAF5C4C — BookStore-Dynapay

### Known SaaS entity IDs (core services only)
- SERVICE-DEA9C3D907BF0B8C — BookStore-Books
- SERVICE-047880462E31FBA8 — BookStore-Orders
- SERVICE-67E64DF15CF80D5F — BookStore-Carts
- SERVICE-3379ADC6B527838F — BookStore-Storage
- SERVICE-4986DAFAFD7A19E7 — BookStore-Ratings
- SERVICE-CAE272892F99D023 — BookStore-Payments
- SERVICE-76233AA3A02CE5E5 — BookStore-Dynapay
- SERVICE-03E504FDA1DBF98A — BookStore-Ingest
- SERVICE-21A157C05E98A8BE — BookStore-Clients
