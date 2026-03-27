package ehrAssist.mapper;

import ehrAssist.entity.MedicationRequestEntity;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus;
import org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestIntent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class MedicationRequestMapper {

    public MedicationRequest toFhirResource(MedicationRequestEntity entity) {
        MedicationRequest request = new MedicationRequest();

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
            request.setStatus(MedicationRequestStatus.fromCode(entity.getStatus()));
        }

        if (entity.getIntent() != null) {
            request.setIntent(MedicationRequestIntent.fromCode(entity.getIntent()));
        }

        if (entity.getPriority() != null) {
            request.setPriority(MedicationRequest.MedicationRequestPriority.fromCode(entity.getPriority()));
        }

        if (entity.getMedicationCode() != null) {
            CodeableConcept medication = new CodeableConcept();
            medication.addCoding()
                    .setSystem(entity.getMedicationCode().getCodeSystem())
                    .setCode(entity.getMedicationCode().getFormularyDrugCd())
                    .setDisplay(entity.getMedicationCode().getCodeDisplay());

            request.setMedication(medication);
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

        if (entity.getDosageText() != null || entity.getDosageRouteCode() != null || entity.getDoseValue() != null) {
            Dosage dosage = request.addDosageInstruction();
            if (entity.getDosageText() != null) {
                dosage.setText(entity.getDosageText());
            }
            if (entity.getDosageRouteCode() != null) {
                CodeableConcept route = new CodeableConcept();
                route.addCoding()
                        .setCode(entity.getDosageRouteCode())
                        .setDisplay(entity.getDosageRouteDisplay());
                dosage.setRoute(route);
            }
            if (entity.getDoseValue() != null) {
                Dosage.DosageDoseAndRateComponent doseAndRate = dosage.addDoseAndRate();
                Quantity doseQuantity = new Quantity()
                        .setValue(entity.getDoseValue())
                        .setUnit(entity.getDoseUnit());
                doseAndRate.setDose(doseQuantity);
            }
        }

        if (entity.getNote() != null) {
            request.addNote().setText(entity.getNote());
        }

        if (entity.getReasonCode() != null) {
            CodeableConcept reason = new CodeableConcept();
            reason.setText(entity.getReasonCode());
            request.addReasonCode(reason);
        }

        return request;
    }

    public MedicationRequestEntity toEntity(MedicationRequest fhir) {
        MedicationRequestEntity entity = new MedicationRequestEntity();

        if (fhir.hasStatus()) {
            entity.setStatus(fhir.getStatus().toCode());
        }

        if (fhir.hasIntent()) {
            entity.setIntent(fhir.getIntent().toCode());
        }

        if (fhir.hasPriority()) {
            entity.setPriority(fhir.getPriority().toCode());
        }

        if (fhir.hasAuthoredOn()) {
            entity.setAuthoredOn(toLocalDateTime(fhir.getAuthoredOn()));
        }

        if (fhir.hasDosageInstruction() && !fhir.getDosageInstruction().isEmpty()) {
            Dosage dosage = fhir.getDosageInstructionFirstRep();
            entity.setDosageText(dosage.getText());
            if (dosage.hasRoute() && dosage.getRoute().hasCoding()) {
                entity.setDosageRouteCode(dosage.getRoute().getCodingFirstRep().getCode());
                entity.setDosageRouteDisplay(dosage.getRoute().getCodingFirstRep().getDisplay());
            }
            if (dosage.hasDoseAndRate() && !dosage.getDoseAndRate().isEmpty()) {
                Type dose = dosage.getDoseAndRateFirstRep().getDose();
                if (dose instanceof Quantity) {
                    Quantity qty = (Quantity) dose;
                    entity.setDoseValue(qty.getValue());
                    entity.setDoseUnit(qty.getUnit());
                }
            }
        }

        if (fhir.hasNote() && !fhir.getNote().isEmpty()) {
            entity.setNote(fhir.getNoteFirstRep().getText());
        }

        if (fhir.hasReasonCode() && !fhir.getReasonCode().isEmpty()) {
            entity.setReasonCode(fhir.getReasonCodeFirstRep().getText());
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
