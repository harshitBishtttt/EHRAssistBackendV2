# API Registry

Roles: ALL = ADMIN+CM+PROVIDER+PATIENT. CM = CARE_MANAGER. "—" = no @PreAuthorize yet.

## Users (`/api/v1/users`)
| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| POST | /login | Public | Login → JWT + refId |
| POST | / | ADMIN | Create user |
| GET | / | ADMIN | List non-admin users |
| GET | /{id} | ADMIN | Get user |
| PUT | /{id} | ADMIN | Update user |
| DELETE | /{id} | ADMIN | Deactivate user |

## Patient (`/baseR4/Patient`)
| Method | Path | Roles | Params |
|--------|------|-------|--------|
| GET | /find | ALL | id |
| GET | / | ADMIN,CM,PROVIDER | _id, family, given, gender, birthdate, email |
| POST | / | — | FHIR JSON body |
| PUT | /{id} | — | FHIR JSON body |
| DELETE | /{id} | — | |
| GET | /ai-recommendation-instructions | ALL | patientId (paginated) |
| GET | /ai-recommended-actions | ALL | patientId (paginated) |
| GET | /{patientId}/clinical-data | ALL | paginated |
| GET | /{patientId}/prediction-data | ADMIN,CM,PROVIDER | |

## Practitioner (`/baseR4/Practitioner`)
| Method | Path | Roles | Params |
|--------|------|-------|--------|
| GET | /{id} | — | |
| GET | / | — | _id, name, specialty |
| GET | /dropdown | — | |
| GET | /fetch-patients-by-practitioner | ADMIN,PROVIDER | id |
| POST | / | — | FHIR JSON body |
| PUT | /{id} | — | FHIR JSON body |
| DELETE | /{id} | — | |
| POST | /ai-recommendation-instructions | ADMIN,CM,PROVIDER | JSON body |
| POST | /ai-recommended-action | ADMIN,CM,PROVIDER | JSON body |

## Organization (`/baseR4/Organization`)
| Method | Path | Roles | Params |
|--------|------|-------|--------|
| GET | /by-care-manager | ADMIN,CM | _id (paginated) |
| GET | /patients | ADMIN,CM | orgId (paginated) |

## Observation (`/baseR4/Observation`)
| Method | Path | Roles | Params |
|--------|------|-------|--------|
| GET | /{id} | ALL | |
| GET | /search | ALL | _id, patient, code, category, value-quantity, date |
| GET | /vitals/search | ALL | patient, code |
| GET | /$risk-feed | ADMIN,CM,PROVIDER | practitionerId, asOfDate |
| POST/PUT/DELETE | standard | — | |

## Standard FHIR CRUD
All have: GET /{id} (ALL), GET search (ALL), POST, PUT, DELETE.
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

Common filters on all: `_id`, `patient`

## EpisodeOfCare (read-only)
GET /{id} (ALL), GET search: _id, patient, status, type

## Other
| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| GET | /baseR4/Measure/$care-gaps | ADMIN,CM,PROVIDER | Care gap evaluation |
| GET | /baseR4/care-plan/tasks | ALL | Care goals by patientId |
| GET | /baseR4/care-plan/lifestyle-goals | ALL | Daily/weekly goals by patientId |
| GET | /baseR4/CareCoordinationNote/search | ADMIN,CM,PROVIDER | Notes search |
| POST | /baseR4/CareCoordinationNote | — | Create note |
| PATCH | /baseR4/CareCoordinationNote | — | Deactivate notes |
| POST | /baseR4/portal/create-recommendations | — | Bulk AI actions |
| GET | /baseR4/portal/task-queue | ADMIN,CM,PROVIDER | Task queue |
| PATCH | /baseR4/portal/update-task | — | Update task status |
| POST | /baseR4/portal/create-review | — | Mark reviewed |
| GET | /baseR4/portal/get-review | ADMIN,CM,PROVIDER | Review status |
| GET | /api/v1/predict/risk-insights | ADMIN,CM,PROVIDER | Risk prediction HTML |
| GET | /baseR4/metadata | Public | CapabilityStatement |
| POST | /baseR4/out-reach/email-portal | — | Send email |
