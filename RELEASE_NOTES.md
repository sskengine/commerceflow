# Release 0.1.0

Initial GitHub-ready CommerceFlow microservice landscape for ECA development.

Implemented:

- 11 independently runnable Spring Boot applications and one contracts module;
- catalog-to-order-to-inventory vertical slice;
- mandatory order currency and money value object;
- atomic in-process inventory reservation;
- idempotent in-process payment authorization;
- baseline delivery, notification, reporting and audit contexts;
- deterministic payment/logistics provider stubs;
- OpenAPI, AsyncAPI, traceability matrix, ADR and controlled ECA experiments;
- Java 21 Maven reactor, Docker Compose and GitHub Actions verification.

Known planned work is documented in `docs/IMPLEMENTATION-STATUS.md`; it is not concealed as completed functionality.
