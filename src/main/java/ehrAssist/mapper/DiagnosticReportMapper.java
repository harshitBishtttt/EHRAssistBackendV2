package ehrAssist.mapper;

import ehrAssist.entity.DiagnosticReportEntity;
import ehrAssist.entity.ObservationEntity;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class DiagnosticReportMapper {

    public DiagnosticReport toFhirResource(DiagnosticReportEntity entity) {
        DiagnosticReport report = new DiagnosticReport();

        report.setId(entity.getId().toString());

        Meta meta = new Meta();
        if (entity.getVersion() != null) {
            meta.setVersionId(entity.getVersion().toString());
        }
        if (entity.getUpdatedAt() != null) {
            meta.setLastUpdated(toDate(entity.getUpdatedAt()));
        }
        report.setMeta(meta);

        if (entity.getStatus() != null) {
            report.setStatus(DiagnosticReportStatus.fromCode(entity.getStatus()));
        }

        if (entity.getCategoryCode() != null) {
            CodeableConcept category = new CodeableConcept();
            category.addCoding()
                    .setCode(entity.getCategoryCode());
            report.addCategory(category);
        }

        if (entity.getCodeSystem() != null || entity.getCodeValue() != null) {
            CodeableConcept code = new CodeableConcept();
            code.addCoding()
                    .setSystem(entity.getCodeSystem())
                    .setCode(entity.getCodeValue())
                    .setDisplay(entity.getCodeDisplay());
            report.setCode(code);
        }

        if (entity.getPatient() != null) {
            report.setSubject(new Reference("Patient/" + entity.getPatient().getId()));
        }

        if (entity.getEncounter() != null) {
            report.setEncounter(new Reference("Encounter/" + entity.getEncounter().getId()));
        }

        if (entity.getPerformer() != null) {
            report.addPerformer(new Reference("Practitioner/" + entity.getPerformer().getId()));
        }

        if (entity.getEffectiveDate() != null) {
            report.setEffective(new DateTimeType(toDate(entity.getEffectiveDate())));
        }

        if (entity.getIssued() != null) {
            report.setIssued(toDate(entity.getIssued()));
        }

        if (entity.getConclusion() != null) {
            report.setConclusion(entity.getConclusion());
        }

        if (entity.getObservations() != null) {
            for (ObservationEntity obs : entity.getObservations()) {
                report.addResult(new Reference("Observation/" + obs.getId()));
            }
        }

        return report;
    }

    public DiagnosticReportEntity toEntity(DiagnosticReport fhir) {
        DiagnosticReportEntity entity = new DiagnosticReportEntity();

        if (fhir.hasStatus()) {
            entity.setStatus(fhir.getStatus().toCode());
        }

        if (fhir.hasCategory() && !fhir.getCategory().isEmpty()) {
            Coding catCoding = fhir.getCategoryFirstRep().getCodingFirstRep();
            entity.setCategoryCode(catCoding.getCode());
        }

        if (fhir.hasCode() && fhir.getCode().hasCoding()) {
            Coding coding = fhir.getCode().getCodingFirstRep();
            entity.setCodeSystem(coding.getSystem());
            entity.setCodeValue(coding.getCode());
            entity.setCodeDisplay(coding.getDisplay());
        }

        if (fhir.hasEffectiveDateTimeType()) {
            entity.setEffectiveDate(toLocalDateTime(fhir.getEffectiveDateTimeType().getValue()));
        }

        if (fhir.hasIssued()) {
            entity.setIssued(toLocalDateTime(fhir.getIssued()));
        }

        if (fhir.hasConclusion()) {
            entity.setConclusion(fhir.getConclusion());
        }

        return entity;
    }

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
