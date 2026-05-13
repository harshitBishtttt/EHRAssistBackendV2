# EHRAssist Backend V2

Spring Boot + HAPI FHIR R4 + SQL Server + JWT (HS256). Maven build, HTTPS port 3001.

## Layers
Controller → Service (interface+impl) → Repository → SQL Server. Mappers convert Entity ↔ FHIR Resource. BundleBuilder wraps lists. FhirResponseHelper serializes responses.

## Packages
- `controller/` — REST endpoints
- `service/` + `impl/` — business logic
- `repository/` + `master/` — JPA repos
- `entity/` + `master/` — JPA entities
- `mapper/` — Entity ↔ FHIR
- `dto/request/`, `dto/response/`, `dto/projection/`
- `util/` — BundleBuilder, FhirResponseHelper, search parsers
- `config/` — FhirConfig, OpenApiConfig
- `security/jwt/` — JwtUtil, JwtAuthFilter
- `exception/` — GlobalExceptionHandler

## Roles
| Role | Ref Column in `user_account` |
|------|------------------------------|
| ADMIN | None |
| CARE_MANAGER | `practitioner_ref_id` |
| PROVIDER | `practitioner_ref_id` |
| PATIENT | `patient_ref_id` |

PROVIDER and CARE_MANAGER both link to the `practitioner` table.

## Auth
Login → JWT (userId, email, role, refId) → JwtAuthFilter → @PreAuthorize on endpoints.

## URL Patterns
- `/baseR4/{Resource}` — FHIR clinical APIs
- `/api/v1/users` — user management
- `/api/v1/predict` — prediction
- `/baseR4/portal` — AI 360

## Responses
- Clinical → `application/fhir+json` via FhirResponseHelper
- Lists → FHIR Bundle (searchset) with pagination via BundleBuilder
- Errors → FHIR OperationOutcome via GlobalExceptionHandler

## Key Decisions
- `verified_at` gates AI recommendation visibility (no separate status column)
- AI Instructions = parent/child entities (ordered bullets); AI Actions = single flat entity
- Controller grouping: no per-feature controllers
- BaseEntity (@MappedSuperclass): id, version, createdAt, updatedAt
- Code master tables for LOINC, ICD-10, CPT, medication reference data
- Interface projections for lightweight native queries

## External
- Risk Insights: REST to external prediction API
- Email: Gmail SMTP
- SSL: custom truststore

## Documentation
- The complete API & Database documentation PDF is stored at `C:\Users\Harshit\Downloads\EHRAssist_API_Documentation.pdf` (generated from HTML via Edge headless).
- **Do NOT keep generated documentation files (PDFs, HTML docs) inside the project directory.** Always output them to the user's Downloads folder (`C:\Users\Harshit\Downloads\`).
