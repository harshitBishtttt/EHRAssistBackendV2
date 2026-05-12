# API Reference

Base URL: `https://<host>:3001`. Clinical APIs return `application/fhir+json`. All list APIs paginated (default page=0, size=10).

## Authentication
```
POST /api/v1/users/login  { "email": "...", "password": "..." }
→ { token, tokenType, expiresInMs, userId, email, role, refId }
```

## User Management — ADMIN only (`/api/v1/users`)
POST / (create) | GET / (list) | GET /{id} | PUT /{id} | DELETE /{id}

## Patient (`/baseR4/Patient`)
| Method | Path | Roles | Params |
|--------|------|-------|--------|
| GET | /find | ALL | id |
| GET | / | ADMIN,CM,PROVIDER | _id, family, given, gender, birthdate, email |
| POST | / | — | FHIR Patient JSON |
| PUT | /{id} | — | FHIR Patient JSON |
| DELETE | /{id} | — | |
| GET | /ai-recommendation-instructions | ALL | patientId |
| GET | /ai-recommended-actions | ALL | patientId |
| GET | /{patientId}/clinical-data | ALL | |
| GET | /{patientId}/prediction-data | ADMIN,CM,PROVIDER | |

## Practitioner (`/baseR4/Practitioner`)
| Method | Path | Roles | Params |
|--------|------|-------|--------|
| GET | /{id} | — | |
| GET | / | — | _id, name, specialty |
| GET | /dropdown | — | |
| GET | /fetch-patients-by-practitioner | ADMIN,PROVIDER | id |
| POST/PUT/DELETE | standard CRUD | — | |
| POST | /ai-recommendation-instructions | ADMIN,CM,PROVIDER | { patientId, practitionerId, payloads[] } |
| POST | /ai-recommended-action | ADMIN,CM,PROVIDER | { patientId, practitionerId, title, description, priority, urgencyNote } |

## Organization (`/baseR4/Organization`)
GET /by-care-manager (ADMIN,CM) — _id | GET /patients (ADMIN,CM) — orgId

## Observation (`/baseR4/Observation`)
GET /{id} (ALL) | GET /search (ALL) — _id, patient, code, category, value-quantity, date | GET /vitals/search (ALL) — patient, code | GET /$risk-feed (ADMIN,CM,PROVIDER) — practitionerId, asOfDate | POST/PUT/DELETE standard

## Standard FHIR CRUD
All: GET /{id} (ALL), GET search (ALL), POST, PUT, DELETE. Common filters: `_id`, `patient`.

| Resource | Extra Search Params |
|----------|---------------------|
| Condition | code |
| Encounter | status, class, date |
| MedicationRequest | status, formulary-drug-cd |
| Procedure | code (CPT int) |
| AllergyIntolerance | — |
| Appointment | status |
| DiagnosticReport | — |
| DocumentReference | type.coding |
| FamilyMemberHistory | relationship |
| Immunization | — |
| ServiceRequest | — |

## EpisodeOfCare (read-only)
GET /{id} (ALL) | GET search: _id, patient, status, type

## Other APIs
| Endpoint | Roles | Description |
|----------|-------|-------------|
| GET /baseR4/Measure/$care-gaps | ADMIN,CM,PROVIDER | Care gap evaluation |
| GET /baseR4/care-plan/tasks | ALL | Care goals |
| GET /baseR4/care-plan/lifestyle-goals | ALL | Daily/weekly goals |
| GET /baseR4/CareCoordinationNote/search | ADMIN,CM,PROVIDER | Notes search |
| POST /baseR4/CareCoordinationNote | — | Create note |
| PATCH /baseR4/CareCoordinationNote | — | Deactivate notes |
| POST /baseR4/portal/create-recommendations | — | Bulk AI actions |
| GET /baseR4/portal/task-queue | ADMIN,CM,PROVIDER | Task queue |
| PATCH /baseR4/portal/update-task | — | Update task |
| POST /baseR4/portal/create-review | — | Mark reviewed |
| GET /baseR4/portal/get-review | ADMIN,CM,PROVIDER | Review status |
| GET /api/v1/predict/risk-insights | ADMIN,CM,PROVIDER | Risk prediction |
| GET /baseR4/metadata | Public | CapabilityStatement |
| POST /baseR4/out-reach/email-portal | — | Send email |
