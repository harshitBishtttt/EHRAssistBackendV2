package ehrAssist.mapper;

import ehrAssist.entity.EncounterEntity;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Encounter.EncounterStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class EncounterMapper {

    public Encounter toFhirResource(EncounterEntity entity) {
        Encounter encounter = new Encounter();

        encounter.setId(entity.getId().toString());

        Meta meta = new Meta();
        if (entity.getVersion() != null) {
            meta.setVersionId(entity.getVersion().toString());
        }
        if (entity.getUpdatedAt() != null) {
            meta.setLastUpdated(toDate(entity.getUpdatedAt()));
        }
        encounter.setMeta(meta);

        if (entity.getStatus() != null) {
            encounter.setStatus(EncounterStatus.fromCode(entity.getStatus()));
        }

        if (entity.getEncounterClass() != null) {
            encounter.setClass_(new Coding()
                    .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                    .setCode(entity.getEncounterClass()));
        }

        if (entity.getTypeDisplay() != null) {
            CodeableConcept type = new CodeableConcept();
            type.setText(entity.getTypeDisplay());
            encounter.addType(type);
        }

        if (entity.getPatient() != null) {
            encounter.setSubject(new Reference("Patient/" + entity.getPatient().getId()));
        }

        if (entity.getPractitioner() != null) {
            Encounter.EncounterParticipantComponent participant = encounter.addParticipant();
            participant.setIndividual(
                    new Reference("Practitioner/" + entity.getPractitioner().getId()));
        }

        Period period = new Period();
        if (entity.getPeriodStart() != null) {
            period.setStart(toDate(entity.getPeriodStart()));
        }
        if (entity.getPeriodEnd() != null) {
            period.setEnd(toDate(entity.getPeriodEnd()));
        }
        encounter.setPeriod(period);

        if (entity.getAdmissionLocation() != null) {
            Encounter.EncounterLocationComponent admitLoc = encounter.addLocation();
            admitLoc.setLocation(new Reference().setDisplay(entity.getAdmissionLocation()));
        }

        if (entity.getDischargeLocation() != null) {
            Encounter.EncounterLocationComponent dischargeLoc = encounter.addLocation();
            dischargeLoc.setLocation(new Reference().setDisplay(entity.getDischargeLocation()));
        }

        if (entity.getDiagnosisText() != null) {
            Encounter.DiagnosisComponent diagnosis = encounter.addDiagnosis();
            diagnosis.setCondition(new Reference().setDisplay(entity.getDiagnosisText()));
        }

        if (entity.getInsurance() != null) {
            encounter.addExtension("insurance", new StringType(entity.getInsurance()));
        }

        if (entity.getClinicalNotes() != null) {
            encounter.addExtension("clinicalNotes", new StringType(entity.getClinicalNotes()));
        }

        return encounter;
    }

    public EncounterEntity toEntity(Encounter fhir) {
        EncounterEntity entity = new EncounterEntity();

        if (fhir.hasStatus()) {
            entity.setStatus(fhir.getStatus().toCode());
        }

        if (fhir.hasClass_()) {
            entity.setEncounterClass(fhir.getClass_().getCode());
        }

        if (fhir.hasType() && !fhir.getType().isEmpty()) {
            entity.setTypeDisplay(fhir.getTypeFirstRep().getText());
        }

        if (fhir.hasPeriod()) {
            if (fhir.getPeriod().hasStart()) {
                entity.setPeriodStart(toLocalDateTime(fhir.getPeriod().getStart()));
            }
            if (fhir.getPeriod().hasEnd()) {
                entity.setPeriodEnd(toLocalDateTime(fhir.getPeriod().getEnd()));
            }
        }

        if (fhir.hasLocation()) {
            for (int i = 0; i < fhir.getLocation().size(); i++) {
                String display = fhir.getLocation().get(i).getLocation().getDisplay();
                if (i == 0) {
                    entity.setAdmissionLocation(display);
                } else if (i == 1) {
                    entity.setDischargeLocation(display);
                }
            }
        }

        if (fhir.hasDiagnosis() && !fhir.getDiagnosis().isEmpty()) {
            entity.setDiagnosisText(fhir.getDiagnosisFirstRep().getCondition().getDisplay());
        }

        Extension insuranceExt = fhir.getExtensionByUrl("insurance");
        if (insuranceExt != null) {
            entity.setInsurance(insuranceExt.getValue().primitiveValue());
        }

        Extension clinicalNotesExt = fhir.getExtensionByUrl("clinicalNotes");
        if (clinicalNotesExt != null) {
            entity.setClinicalNotes(clinicalNotesExt.getValue().primitiveValue());
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
