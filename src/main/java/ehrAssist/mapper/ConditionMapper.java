package ehrAssist.mapper;

import ehrAssist.entity.ConditionEntity;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class ConditionMapper {

    public Condition toFhirResource(ConditionEntity entity) {
        Condition condition = new Condition();

        condition.setId(entity.getId().toString());

        Meta meta = new Meta();
        if (entity.getVersion() != null) {
            meta.setVersionId(entity.getVersion().toString());
        }
        if (entity.getUpdatedAt() != null) {
            meta.setLastUpdated(toDate(entity.getUpdatedAt()));
        }
        condition.setMeta(meta);

        if (entity.getClinicalStatus() != null) {
            CodeableConcept clinicalStatus = new CodeableConcept();
            clinicalStatus.addCoding()
                    .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                    .setCode(entity.getClinicalStatus());
            condition.setClinicalStatus(clinicalStatus);
        }

        if (entity.getVerificationStatus() != null) {
            CodeableConcept verificationStatus = new CodeableConcept();
            verificationStatus.addCoding()
                    .setSystem("http://terminology.hl7.org/CodeSystem/condition-ver-status")
                    .setCode(entity.getVerificationStatus());
            condition.setVerificationStatus(verificationStatus);
        }

        if (entity.getCodeMaster() != null) {
            if (entity.getCodeMaster().getCategoryCode() != null) {
                CodeableConcept category = new CodeableConcept();
                category.addCoding()
                        .setCode(entity.getCodeMaster().getCategoryCode())
                        .setDisplay(entity.getCodeMaster().getCategory());
                condition.addCategory(category);
            }

            CodeableConcept code = new CodeableConcept();
            code.addCoding()
                    .setSystem(entity.getCodeMaster().getCodeSystem())
                    .setCode(entity.getCodeMaster().getCodeValue() != null
                            ? entity.getCodeMaster().getCodeValue().trim() : null)
                    .setDisplay(entity.getCodeMaster().getLongTitle());
            condition.setCode(code);
        }

        if (entity.getSeverityCode() != null) {
            CodeableConcept severity = new CodeableConcept();
            severity.addCoding()
                    .setSystem("http://snomed.info/sct")
                    .setCode(entity.getSeverityCode())
                    .setDisplay(entity.getSeverityDisplay());
            condition.setSeverity(severity);
        }

        if (entity.getPatient() != null) {
            condition.setSubject(new Reference("Patient/" + entity.getPatient().getId()));
        }

        if (entity.getEncounter() != null) {
            condition.setEncounter(new Reference("Encounter/" + entity.getEncounter().getId()));
        }

        if (entity.getRecorder() != null) {
            condition.setRecorder(new Reference("Practitioner/" + entity.getRecorder().getId()));
        }

        if (entity.getOnsetDate() != null) {
            condition.setOnset(new DateTimeType(toDate(entity.getOnsetDate())));
        }

        if (entity.getAbatementDate() != null) {
            condition.setAbatement(new DateTimeType(toDate(entity.getAbatementDate())));
        }

        if (entity.getRecordedDate() != null) {
            condition.setRecordedDate(toDate(entity.getRecordedDate()));
        }

        return condition;
    }

    public ConditionEntity toEntity(Condition fhir) {
        ConditionEntity entity = new ConditionEntity();

        if (fhir.hasClinicalStatus() && fhir.getClinicalStatus().hasCoding()) {
            entity.setClinicalStatus(fhir.getClinicalStatus().getCodingFirstRep().getCode());
        }

        if (fhir.hasVerificationStatus() && fhir.getVerificationStatus().hasCoding()) {
            entity.setVerificationStatus(fhir.getVerificationStatus().getCodingFirstRep().getCode());
        }

        if (fhir.hasSeverity() && fhir.getSeverity().hasCoding()) {
            entity.setSeverityCode(fhir.getSeverity().getCodingFirstRep().getCode());
            entity.setSeverityDisplay(fhir.getSeverity().getCodingFirstRep().getDisplay());
        }

        if (fhir.hasOnset() && fhir.getOnset() instanceof DateTimeType) {
            entity.setOnsetDate(toLocalDate(((DateTimeType) fhir.getOnset()).getValue()));
        }

        if (fhir.hasAbatement() && fhir.getAbatement() instanceof DateTimeType) {
            entity.setAbatementDate(toLocalDate(((DateTimeType) fhir.getAbatement()).getValue()));
        }

        if (fhir.hasRecordedDate()) {
            entity.setRecordedDate(toLocalDate(fhir.getRecordedDate()));
        }

        return entity;
    }

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
