# Database Schema

SQL Server, `dbo` schema. BaseEntity = id (UUID PK), version, created_at, updated_at.

## Core Tables

**patient** (BaseEntity) — active, gender, birth_date, deceased_flag/date, marital_status_code/display, language_code/display, primary_practitioner_id (FK→practitioner), managing_organization_id (FK→organization), disease
- Children: patient_name, patient_address, patient_telecom, patient_identifier

**practitioner** (BaseEntity) — family_name, given_name, gender, npi (UNIQUE), specialty_code/display, phone, email, birth_date, organization_id (FK→organization), active

**organization** (BaseEntity) — name, type_code/display, phone, address_city/state, active

## Clinical Tables (all BaseEntity, all have patient_id FK)

**observation** — observation_code_id (FK→observation_code_master.item_id), status, value_quantity/unit/string, interpretation_code, effective_date, issued
**condition** — condition_code_id (FK→condition_code_master), clinical_status, verification_status, severity, onset/abatement/recorded dates
**encounter** — status, encounter_class, type, period, admission/discharge, reason, diagnosis_text, insurance, clinical_notes
**medication_request** — medication_code_id (FK→medication_code_master), status, intent, priority, dosage, reason, authored_on
**procedure** — procedure_code_id (FK→procedure_code_master), cpt_code, status, body_site, outcome, performed period
**allergy_intolerance** — clinical/verification status, type, category, criticality, code, onset_date
**appointment** — practitioner_id, status, service_type, reason, start/end_time
**diagnostic_report** — status, category, code, effective_date, conclusion. Join: diagnostic_report_observation
**document_reference** — status, type, content, period
**family_member_history** — relationship, conditions (child: family_member_history_condition)
**immunization** — vaccine code, occurrence_date, lot, site/route, dose
**service_request** — status, intent, category, code, authored_on

## Episode of Care
- episode_of_care (BaseEntity) — patient, org, care_manager, type, period, status
- episode_of_care_diagnosis — episode_id, condition_id, role, rank
- episode_of_care_encounter — composite PK (episode_id, encounter_id)
- episode_of_care_status_history — episode_id, status, period

## Code Masters (reference data, identity PK)
- observation_code_master — item_id (UNIQUE), code_system, loinc_code, category, reference ranges
- condition_code_master — code_system, code_value, icd10_code, category
- medication_code_master — code_system, formulary_drug_cd, form_code
- procedure_code_master — category, section ranges, min/max_code

## AI Tables
- ai_recommendation_instructions — patient_id, practitioner_id, category, verified_at
- ai_recommendation_instructions_payload — recommendation_id (FK), sequence, content_string
- ai_recommended_action — patient_id, practitioner_id, title, description, priority, urgency_note, verified_at, verified_by
- patient_360_ai_recommended_actions — patient_id, status, priority, action, description, ai_rationale, due_date, created_at

## Care Management
- care_manager_organization_mapper — care_manager_id (FK→practitioner), organization_id (FK→organization). UNIQUE(care_manager_id, organization_id)
- care_coordination_notes — patient_id, coordinator_email/name/role, care_notes, status, is_active, recommendation_note_id
- care_goals — patient_id, status, kind, display, description, scheduled_start/end
- lifestyle_goal — patient_id, steps, water_intake_glasses, exercise_minutes, scheduled_start/end
- mark_reviewed — patient_id, is_reviewed, created_date

## System
- user_account — email (UNIQUE), password_hash, role, is_active, failed_login_attempts, patient_ref_id (FK→patient), practitioner_ref_id (FK→practitioner)
- fhir_audit_logs — action, outcome, agent info, request path, response status, execution_time_ms
- risk_insights_cashing — patient_id, observation/condition dates (cache key), report_html
- vitals — patient_id, observation_code_id (FK→observation_code_master.item_id), value, effective_date
