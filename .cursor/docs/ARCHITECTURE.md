# Architecture

## Overview
EHRAssist Backend V2 — Spring Boot backend exposing EHR data through HL7 FHIR R4 APIs for patients, providers, care managers, and administrators.

## Tech Stack
| Component | Technology |
|-----------|------------|
| Language | Java 17+ |
| Framework | Spring Boot |
| Database | Microsoft SQL Server |
| ORM | Spring Data JPA / Hibernate |
| FHIR | HAPI FHIR R4 |
| Auth | JWT (HS256) + Spring Security |
| Build | Maven |
| Docs | SpringDoc OpenAPI (Swagger) |

## Application Layers
```
Controller → Service (interface+impl) → Repository → SQL Server
                      ↓
               Mapper (Entity ↔ FHIR Resource)
                      ↓
            BundleBuilder / FhirResponseHelper
```

## Roles
| Role | Description | Ref in user_account |
|------|-------------|---------------------|
| ADMIN | Full access | None |
| CARE_MANAGER | Care coordination | practitioner_ref_id |
| PROVIDER | Doctor | practitioner_ref_id |
| PATIENT | Own data | patient_ref_id |

Both PROVIDER and CARE_MANAGER link to the `practitioner` table.

## Auth Flow
1. `POST /api/v1/users/login` → validates credentials → returns JWT (userId, email, role, refId)
2. Client sends `Authorization: Bearer <token>`
3. `JwtAuthFilter` validates → sets SecurityContext
4. `@PreAuthorize` checks role per endpoint

## FHIR R4 Compliance
- Clinical APIs → `application/fhir+json`
- Lists → FHIR Bundle (searchset) with pagination
- Errors → FHIR OperationOutcome
- Custom fields → FHIR Extensions (disease, AI verification, action metadata)

## Key Design Decisions
1. `verified_at` gates AI content visibility — no separate status field
2. AI Instructions = parent/child entities (ordered bullets); AI Actions = flat entity
3. Endpoints grouped in existing controllers — no per-feature controllers
4. Code master tables for reference data (LOINC, ICD-10, CPT, medication)
5. Interface projections for lightweight native queries
6. N+1 prevention is use-case based — optimize heavy APIs, accept for simple lookups
