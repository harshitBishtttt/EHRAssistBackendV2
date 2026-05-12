# Changelog

## Features
- FHIR CRUD for 16 resource types
- JWT auth (4 roles: ADMIN, CARE_MANAGER, PROVIDER, PATIENT), @PreAuthorize on 54+ endpoints
- Risk Feed ($risk-feed) — 6-month date-window, native CTE query
- AI Recommendation Instructions — parent/child entities, doctor verification, FHIR Communication
- AI Recommended Actions — flat entity, same verification flow, FHIR Communication
- Care Manager ↔ Organization mapping — native JOIN + projection
- Organization → Patients API with total count
- Patient disease column → FHIR Extension
- Care Gaps evaluation (diabetes, hypertension, CKD) → MeasureReport
- Portal AI 360 — task queue, care coordination notes, mark-reviewed
- Lifestyle Goals — daily/weekly tracking → FHIR CarePlan
- Clinical Data Aggregation API
- Prediction Data API for external ML service
- Risk Insights — cached external prediction HTML
- Email outreach via Gmail SMTP
- FHIR Audit Logging

## Bug Fixes
- Observation N+1 (JoinColumn item_id fix)
- Lifestyle Goal column name (scheduled_start)
- Patient GET → /find?id= (request param)
- Exception handler 500 → proper 403/401
- SQL Server filtered unique indexes for NULL handling
- Removed redundant PatientEverythingController
- Removed isEmailVerified/lockedUntil from user entities
