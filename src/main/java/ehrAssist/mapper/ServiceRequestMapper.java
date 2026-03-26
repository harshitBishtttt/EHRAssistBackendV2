package ehrAssist.mapper;

import ehrAssist.entity.ServiceRequestEntity;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestStatus;
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestIntent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class ServiceRequestMapper {

    public ServiceRequest toFhirResource(ServiceRequestEntity entity) {
        ServiceRequest request = new ServiceRequest();

        request.setId(entity.getId().toString());

        Meta meta = new Meta();
        if (entity.getVersion() != null) {
            meta.setVersionId(entity.getVersion().toString());
        }
        if (entity.getUpdatedAt() != null) {
            meta.setLastUpdated(toDate(entity.getUpdatedAt()));
        }
        request.setMeta(meta);

        if (entity.getStatus() != null) {
            request.setStatus(ServiceRequestStatus.fromCode(entity.getStatus()));
        }

        if (entity.getIntent() != null) {
            request.setIntent(ServiceRequestIntent.fromCode(entity.getIntent()));
        }

        if (entity.getCategoryCode() != null) {
            CodeableConcept category = new CodeableConcept();
            category.addCoding()
                    .setCode(entity.getCategoryCode());
            request.addCategory(category);
        }

        if (entity.getCodeSystem() != null || entity.getCodeValue() != null) {
            CodeableConcept code = new CodeableConcept();
            code.addCoding()
                    .setSystem(entity.getCodeSystem())
                    .setCode(entity.getCodeValue())
                    .setDisplay(entity.getCodeDisplay());
            request.setCode(code);
        }

        if (entity.getPatient() != null) {
            request.setSubject(new Reference("Patient/" + entity.getPatient().getId()));
        }

        if (entity.getEncounter() != null) {
            request.setEncounter(new Reference("Encounter/" + entity.getEncounter().getId()));
        }

        if (entity.getRequester() != null) {
            request.setRequester(new Reference("Practitioner/" + entity.getRequester().getId()));
        }

        if (entity.getAuthoredOn() != null) {
            request.setAuthoredOn(toDate(entity.getAuthoredOn()));
        }

        if (entity.getNote() != null) {
            request.addNote().setText(entity.getNote());
        }

        return request;
    }

    public ServiceRequestEntity toEntity(ServiceRequest fhir) {
        ServiceRequestEntity entity = new ServiceRequestEntity();

        if (fhir.hasStatus()) {
            entity.setStatus(fhir.getStatus().toCode());
        }

        if (fhir.hasIntent()) {
            entity.setIntent(fhir.getIntent().toCode());
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

        if (fhir.hasAuthoredOn()) {
            entity.setAuthoredOn(toLocalDateTime(fhir.getAuthoredOn()));
        }

        if (fhir.hasNote() && !fhir.getNote().isEmpty()) {
            entity.setNote(fhir.getNoteFirstRep().getText());
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
