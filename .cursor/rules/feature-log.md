# Feature Log

## Core
- FHIR CRUD for 16 resources (Patient, Practitioner, Observation, Condition, Encounter, MedicationRequest, Procedure, AllergyIntolerance, Appointment, DiagnosticReport, DocumentReference, FamilyMemberHistory, Immunization, ServiceRequest, EpisodeOfCare [read-only], Organization)
- JWT auth with 4 roles, `@PreAuthorize` on 54+ endpoints
- GlobalExceptionHandler → FHIR OperationOutcome

## AI Features
- **Instructions**: parent/child (ai_recommendation_instructions + payload). Create via PractitionerController, Get via PatientController. `verified_at` gates visibility. FHIR Communication.
- **Actions**: flat entity (ai_recommended_action). Same create/get pattern. Extensions for title/priority/urgency. FHIR Communication.
- **Portal AI 360**: patient_360_ai_recommended_actions, task queue, care coordination notes, mark-reviewed

## Clinical
- **Risk Feed**: $risk-feed with 6-month date-window, native CTE query
- **Care Gaps**: diabetes/hypertension/CKD evaluation → MeasureReport
- **Vitals**: separate vitals table + search API
- **Clinical Data Aggregation**: `/Patient/{id}/clinical-data`
- **Prediction Data**: structured map for external ML API
- **Lifestyle Goals**: daily steps/water/exercise with weekly rollups → FHIR CarePlan

## Care Management
- care_manager_organization_mapper (many-to-many) → native JOIN + projection
- Organization patients API with total count

## Fixes Applied
- Observation N+1 (JoinColumn item_id fix)
- Lifestyle Goal column name (scheduled_start)
- Patient GET → /find?id= (request param)
- Exception handler 500 → proper 403/401
- SQL Server filtered unique indexes for NULL handling
