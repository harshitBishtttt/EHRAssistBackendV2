# Database Schema

SQL Server, `dbo` schema.

## Base Entity Pattern
Most clinical entities inherit: `id` (UUID PK), `version` (optimistic lock), `created_at`, `updated_at`.

## Entity Relationship
```
organization ──┬── manages ──→ patient ──→ clinical resources
               ├── employs ──→ practitioner ──→ (performs/records)
               └── care_manager_organization_mapper
```

## Core Tables

**patient** (BaseEntity) — active, gender, birth_date, deceased_flag/date, marital_status, language, disease, primary_practitioner_id (FK), managing_organization_id (FK)
- Children: patient_name, patient_address, patient_telecom, patient_identifier

**practitioner** (BaseEntity) — family_name, given_name, gender, npi (UNIQUE), specialty, phone, email, birth_date, organization_id (FK), active

**organization** (BaseEntity) — name, type_code/display, phone, address_city/state, active

## Clinical Tables (all BaseEntity with patient_id FK)

| Table | Key Columns |
|-------|-------------|
| observation | observation_code_id (→ code_master.item_id), value_quantity/unit/string, effective_date, issued |
| condition | condition_code_id (→ code_master), clinical_status, severity, onset/abatement dates |
| encounter | status, class, type, period, admission/discharge, diagnosis_text, clinical_notes |
| medication_request | medication_code_id (→ code_master), status, intent, dosage, authored_on |
| procedure | procedure_code_id (→ code_master), cpt_code, status, body_site, performed period |
| allergy_intolerance | clinical/verification status, type, category, criticality, code |
| appointment | practitioner_id, status, service_type, start/end_time |
| diagnostic_report | status, category, code, conclusion. Join: diagnostic_report_observation |
| document_reference | status, type, content, period |
| family_member_history | relationship, conditions (child table) |
| immunization | vaccine code, occurrence_date, dose |
| service_request | status, intent, code, authored_on |

## Episode of Care
episode_of_care (BaseEntity) + diagnosis, encounter (composite PK), status_history tables

## Code Masters (identity PK, reference data)
observation_code_master (item_id UNIQUE, loinc_code), condition_code_master (icd10_code), medication_code_master (formulary_drug_cd), procedure_code_master (min/max_code)

## AI Tables
- ai_recommendation_instructions + payload (parent/child, verified_at gates visibility)
- ai_recommended_action (flat, title/description/priority/urgency_note, verified_at/by)
- patient_360_ai_recommended_actions (Portal AI system, status-based task queue)

## Care Management
- care_manager_organization_mapper — UNIQUE(care_manager_id, organization_id)
- care_coordination_notes, care_goals, lifestyle_goal, mark_reviewed

## System
- user_account — email, password_hash, role, patient_ref_id, practitioner_ref_id
- fhir_audit_logs — request/response tracking
- risk_insights_cashing — cached prediction HTML
- vitals — separate observation-like table for vitals
