package ehrAssist.mapper;

import ehrAssist.entity.FamilyMemberHistoryConditionEntity;
import ehrAssist.entity.FamilyMemberHistoryEntity;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.FamilyMemberHistory.FamilyHistoryStatus;
import org.hl7.fhir.r4.model.FamilyMemberHistory.FamilyMemberHistoryConditionComponent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class FamilyMemberHistoryMapper {

    public FamilyMemberHistory toFhirResource(FamilyMemberHistoryEntity entity) {
        FamilyMemberHistory fmh = new FamilyMemberHistory();

        fmh.setId(entity.getId().toString());

        Meta meta = new Meta();
        if (entity.getVersion() != null) {
            meta.setVersionId(entity.getVersion().toString());
        }
        if (entity.getUpdatedAt() != null) {
            meta.setLastUpdated(toDate(entity.getUpdatedAt()));
        }
        fmh.setMeta(meta);

        if (entity.getStatus() != null) {
            fmh.setStatus(FamilyHistoryStatus.fromCode(entity.getStatus()));
        }

        if (entity.getPatient() != null) {
            fmh.setPatient(new Reference("Patient/" + entity.getPatient().getId()));
        }

        fmh.setName(entity.getName());

        if (entity.getRelationshipCode() != null) {
            CodeableConcept relationship = new CodeableConcept();
            relationship.addCoding()
                    .setSystem(entity.getRelationshipSystem() != null
                            ? entity.getRelationshipSystem()
                            : "http://terminology.hl7.org/CodeSystem/v3-RoleCode")
                    .setCode(entity.getRelationshipCode())
                    .setDisplay(entity.getRelationshipDisplay());
            relationship.setText(entity.getRelationshipDisplay());
            fmh.setRelationship(relationship);
        }

        if (entity.getSex() != null) {
            CodeableConcept sex = new CodeableConcept();
            sex.addCoding()
                    .setSystem("http://hl7.org/fhir/administrative-gender")
                    .setCode(entity.getSex());
            fmh.setSex(sex);
        }

        if (entity.getBornDate() != null) {
            fmh.setBorn(new DateType(toDate(entity.getBornDate())));
        }

        if (entity.getAgeValue() != null) {
            Age age = new Age();
            age.setValue(entity.getAgeValue());
            age.setUnit(entity.getAgeUnit() != null ? entity.getAgeUnit() : "a");
            age.setSystem("http://unitsofmeasure.org");
            age.setCode(entity.getAgeUnit() != null ? entity.getAgeUnit() : "a");
            fmh.setAge(age);
        }

        if (entity.getEstimatedAge() != null) {
            fmh.setEstimatedAge(entity.getEstimatedAge());
        }

        if (entity.getDeceasedDate() != null) {
            fmh.setDeceased(new DateType(toDate(entity.getDeceasedDate())));
        } else if (entity.getDeceasedAgeValue() != null) {
            Age deceasedAge = new Age();
            deceasedAge.setValue(entity.getDeceasedAgeValue());
            deceasedAge.setUnit(entity.getDeceasedAgeUnit() != null ? entity.getDeceasedAgeUnit() : "a");
            deceasedAge.setSystem("http://unitsofmeasure.org");
            deceasedAge.setCode(entity.getDeceasedAgeUnit() != null ? entity.getDeceasedAgeUnit() : "a");
            fmh.setDeceased(deceasedAge);
        } else if (entity.getDeceasedFlag() != null) {
            fmh.setDeceased(new BooleanType(entity.getDeceasedFlag()));
        }

        if (entity.getNote() != null) {
            fmh.addNote(new Annotation().setText(entity.getNote()));
        }

        if (entity.getConditions() != null) {
            for (FamilyMemberHistoryConditionEntity condEntity : entity.getConditions()) {
                fmh.addCondition(mapConditionToFhir(condEntity));
            }
        }

        return fmh;
    }

    private FamilyMemberHistoryConditionComponent mapConditionToFhir(FamilyMemberHistoryConditionEntity entity) {
        FamilyMemberHistoryConditionComponent comp = new FamilyMemberHistoryConditionComponent();

        CodeableConcept code = new CodeableConcept();
        code.addCoding()
                .setSystem(entity.getCodeSystem() != null ? entity.getCodeSystem() : "http://snomed.info/sct")
                .setCode(entity.getCodeValue())
                .setDisplay(entity.getCodeDisplay());
        code.setText(entity.getCodeDisplay());
        comp.setCode(code);

        if (entity.getOutcomeCode() != null) {
            CodeableConcept outcome = new CodeableConcept();
            outcome.addCoding()
                    .setCode(entity.getOutcomeCode())
                    .setDisplay(entity.getOutcomeDisplay());
            comp.setOutcome(outcome);
        }

        if (entity.getContributedToDeath() != null) {
            comp.setContributedToDeath(entity.getContributedToDeath());
        }

        if (entity.getOnsetAgeValue() != null) {
            Age onsetAge = new Age();
            onsetAge.setValue(entity.getOnsetAgeValue());
            onsetAge.setUnit(entity.getOnsetAgeUnit() != null ? entity.getOnsetAgeUnit() : "a");
            onsetAge.setSystem("http://unitsofmeasure.org");
            onsetAge.setCode(entity.getOnsetAgeUnit() != null ? entity.getOnsetAgeUnit() : "a");
            comp.setOnset(onsetAge);
        } else if (entity.getOnsetString() != null) {
            comp.setOnset(new StringType(entity.getOnsetString()));
        }

        if (entity.getNote() != null) {
            comp.addNote(new Annotation().setText(entity.getNote()));
        }

        return comp;
    }

    public FamilyMemberHistoryEntity toEntity(FamilyMemberHistory fhir) {
        FamilyMemberHistoryEntity entity = new FamilyMemberHistoryEntity();

        if (fhir.hasStatus()) {
            entity.setStatus(fhir.getStatus().toCode());
        }

        entity.setName(fhir.getName());

        if (fhir.hasRelationship() && fhir.getRelationship().hasCoding()) {
            Coding rel = fhir.getRelationship().getCodingFirstRep();
            entity.setRelationshipSystem(rel.getSystem());
            entity.setRelationshipCode(rel.getCode());
            entity.setRelationshipDisplay(rel.getDisplay());
        }

        if (fhir.hasSex() && fhir.getSex().hasCoding()) {
            entity.setSex(fhir.getSex().getCodingFirstRep().getCode());
        }

        if (fhir.hasBornDateType()) {
            entity.setBornDate(toLocalDate(fhir.getBornDateType().getValue()));
        }

        if (fhir.hasAgeAge()) {
            entity.setAgeValue(fhir.getAgeAge().getValue());
            entity.setAgeUnit(fhir.getAgeAge().getCode());
        }

        if (fhir.hasEstimatedAge()) {
            entity.setEstimatedAge(fhir.getEstimatedAge());
        }

        if (fhir.hasDeceasedDateType()) {
            entity.setDeceasedDate(toLocalDate(fhir.getDeceasedDateType().getValue()));
        } else if (fhir.hasDeceasedAge()) {
            entity.setDeceasedAgeValue(fhir.getDeceasedAge().getValue());
            entity.setDeceasedAgeUnit(fhir.getDeceasedAge().getCode());
        } else if (fhir.hasDeceasedBooleanType()) {
            entity.setDeceasedFlag(fhir.getDeceasedBooleanType().booleanValue());
        }

        if (fhir.hasNote()) {
            entity.setNote(fhir.getNoteFirstRep().getText());
        }

        if (fhir.hasCondition()) {
            for (FamilyMemberHistoryConditionComponent condComp : fhir.getCondition()) {
                FamilyMemberHistoryConditionEntity condEntity = mapConditionToEntity(condComp);
                condEntity.setFamilyMemberHistory(entity);
                entity.getConditions().add(condEntity);
            }
        }

        return entity;
    }

    private FamilyMemberHistoryConditionEntity mapConditionToEntity(FamilyMemberHistoryConditionComponent comp) {
        FamilyMemberHistoryConditionEntity entity = new FamilyMemberHistoryConditionEntity();

        if (comp.hasCode() && comp.getCode().hasCoding()) {
            Coding coding = comp.getCode().getCodingFirstRep();
            entity.setCodeSystem(coding.getSystem());
            entity.setCodeValue(coding.getCode());
            entity.setCodeDisplay(coding.getDisplay());
        }

        if (comp.hasOutcome() && comp.getOutcome().hasCoding()) {
            Coding outcome = comp.getOutcome().getCodingFirstRep();
            entity.setOutcomeCode(outcome.getCode());
            entity.setOutcomeDisplay(outcome.getDisplay());
        }

        if (comp.hasContributedToDeath()) {
            entity.setContributedToDeath(comp.getContributedToDeath());
        }

        if (comp.hasOnsetAge()) {
            entity.setOnsetAgeValue(comp.getOnsetAge().getValue());
            entity.setOnsetAgeUnit(comp.getOnsetAge().getCode());
        } else if (comp.hasOnsetStringType()) {
            entity.setOnsetString(comp.getOnsetStringType().getValue());
        }

        if (comp.hasNote()) {
            entity.setNote(comp.getNoteFirstRep().getText());
        }

        return entity;
    }

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
