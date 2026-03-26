package ehrAssist.mapper;

import ehrAssist.entity.ProcedureEntity;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Procedure.ProcedureStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class ProcedureMapper {

    public Procedure toFhirResource(ProcedureEntity entity) {
        Procedure procedure = new Procedure();

        procedure.setId(entity.getId().toString());

        Meta meta = new Meta();
        if (entity.getVersion() != null) {
            meta.setVersionId(entity.getVersion().toString());
        }
        if (entity.getUpdatedAt() != null) {
            meta.setLastUpdated(toDate(entity.getUpdatedAt()));
        }
        procedure.setMeta(meta);

        if (entity.getStatus() != null) {
            procedure.setStatus(ProcedureStatus.fromCode(entity.getStatus()));
        }

        if (entity.getCodeMaster() != null) {
            CodeableConcept code = new CodeableConcept();
            code.addCoding()
                    .setSystem(entity.getCodeMaster().getCodeSystem())
                    .setCode(String.valueOf(entity.getCptCode()))
                    .setDisplay(entity.getCodeMaster().getSubsectionHeader());
            if (entity.getDescription() != null) {
                code.setText(entity.getDescription());
            }
            procedure.setCode(code);

            if (entity.getCodeMaster().getSectionHeader() != null) {
                CodeableConcept category = new CodeableConcept();
                category.setText(entity.getCodeMaster().getSectionHeader());
                procedure.setCategory(category);
            }
        }

        if (entity.getPatient() != null) {
            procedure.setSubject(new Reference("Patient/" + entity.getPatient().getId()));
        }

        if (entity.getEncounter() != null) {
            procedure.setEncounter(new Reference("Encounter/" + entity.getEncounter().getId()));
        }

        if (entity.getPerformer() != null) {
            Procedure.ProcedurePerformerComponent performer = procedure.addPerformer();
            performer.setActor(new Reference("Practitioner/" + entity.getPerformer().getId()));
        }

        Period performedPeriod = new Period();
        boolean hasPeriod = false;
        if (entity.getPerformedStart() != null) {
            performedPeriod.setStart(toDate(entity.getPerformedStart()));
            hasPeriod = true;
        }
        if (entity.getPerformedEnd() != null) {
            performedPeriod.setEnd(toDate(entity.getPerformedEnd()));
            hasPeriod = true;
        }
        if (hasPeriod) {
            procedure.setPerformed(performedPeriod);
        }

        if (entity.getBodySiteCode() != null) {
            CodeableConcept bodySite = new CodeableConcept();
            bodySite.addCoding()
                    .setCode(entity.getBodySiteCode())
                    .setDisplay(entity.getBodySiteDisplay());
            procedure.addBodySite(bodySite);
        }

        if (entity.getOutcomeCode() != null) {
            CodeableConcept outcome = new CodeableConcept();
            outcome.addCoding().setCode(entity.getOutcomeCode());
            procedure.setOutcome(outcome);
        }

        return procedure;
    }

    public ProcedureEntity toEntity(Procedure fhir) {
        ProcedureEntity entity = new ProcedureEntity();

        if (fhir.hasStatus()) {
            entity.setStatus(fhir.getStatus().toCode());
        }

        if (fhir.hasCode()) {
            CodeableConcept code = fhir.getCode();
            if (code.hasCoding()) {
                String cptStr = code.getCodingFirstRep().getCode();
                if (cptStr != null) {
                    entity.setCptCode(Integer.parseInt(cptStr));
                }
            }
            if (code.hasText()) {
                entity.setDescription(code.getText());
            }
        }

        if (fhir.hasPerformed() && fhir.getPerformed() instanceof Period) {
            Period period = (Period) fhir.getPerformed();
            if (period.hasStart()) {
                entity.setPerformedStart(toLocalDateTime(period.getStart()));
            }
            if (period.hasEnd()) {
                entity.setPerformedEnd(toLocalDateTime(period.getEnd()));
            }
        }

        if (fhir.hasBodySite() && !fhir.getBodySite().isEmpty()) {
            CodeableConcept bs = fhir.getBodySiteFirstRep();
            if (bs.hasCoding()) {
                entity.setBodySiteCode(bs.getCodingFirstRep().getCode());
                entity.setBodySiteDisplay(bs.getCodingFirstRep().getDisplay());
            } else if (bs.hasText()) {
                entity.setBodySiteDisplay(bs.getText());
            }
        }

        if (fhir.hasOutcome()) {
            if (fhir.getOutcome().hasCoding()) {
                entity.setOutcomeCode(fhir.getOutcome().getCodingFirstRep().getCode());
            } else if (fhir.getOutcome().hasText()) {
                entity.setOutcomeCode(fhir.getOutcome().getText());
            }
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
