package ehrAssist.mapper;

import ehrAssist.entity.VitalsEntity;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class VitalsMapper {

    public Observation toFhirResource(VitalsEntity entity) {
        Observation observation = new Observation();

        observation.setId(entity.getId().toString());

        if (entity.getStatus() != null) {
            observation.setStatus(ObservationStatus.fromCode(entity.getStatus()));
        }

        if (entity.getCodeMaster() != null) {
            if (entity.getCodeMaster().getFhirCategoryCode() != null) {
                CodeableConcept category = new CodeableConcept();
                category.addCoding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                        .setCode(entity.getCodeMaster().getFhirCategoryCode());
                observation.addCategory(category);
            }

            CodeableConcept code = new CodeableConcept();
            code.addCoding()
                    .setSystem(entity.getCodeMaster().getCodeSystem())
                    .setCode(entity.getCodeMaster().getLoincCode())
                    .setDisplay(entity.getCodeMaster().getCodeDisplay());
            code.setText(entity.getCodeMaster().getCodeDisplay());
            observation.setCode(code);
        }

        if (entity.getPatientId() != null) {
            observation.setSubject(new Reference("Patient/" + entity.getPatientId()));
        }

        if (entity.getEncounterId() != null) {
            observation.setEncounter(new Reference("Encounter/" + entity.getEncounterId()));
        }

        if (entity.getEffectiveDate() != null) {
            observation.setEffective(new DateTimeType(toDate(entity.getEffectiveDate())));
        }

        if (entity.getValueQuantity() != null) {
            String unit = entity.getValueUnit();
            if (unit == null && entity.getCodeMaster() != null) {
                unit = entity.getCodeMaster().getExpectedUnit();
            }
            Quantity quantity = new Quantity()
                    .setValue(entity.getValueQuantity())
                    .setUnit(unit)
                    .setSystem("http://unitsofmeasure.org");
            observation.setValue(quantity);
        } else if (entity.getValueString() != null) {
            observation.setValue(new StringType(entity.getValueString()));
        }

        if (entity.getInterpretationCode() != null) {
            CodeableConcept interpretation = new CodeableConcept();
            interpretation.addCoding().setCode(entity.getInterpretationCode());
            observation.addInterpretation(interpretation);
        }

        if (entity.getCodeMaster() != null) {
            BigDecimal low = entity.getCodeMaster().getReferenceRangeLow();
            BigDecimal high = entity.getCodeMaster().getReferenceRangeHigh();
            if (low != null || high != null) {
                Observation.ObservationReferenceRangeComponent range = observation.addReferenceRange();
                if (low != null) range.setLow(new Quantity().setValue(low));
                if (high != null) range.setHigh(new Quantity().setValue(high));
            }
        }

        if (entity.getCodeMaster() != null || entity.getValueQuantity() != null || entity.getValueString() != null) {
            Observation.ObservationComponentComponent component = observation.addComponent();
            if (entity.getCodeMaster() != null) {
                CodeableConcept compCode = new CodeableConcept();
                compCode.addCoding()
                        .setSystem(entity.getCodeMaster().getCodeSystem())
                        .setCode(entity.getCodeMaster().getLoincCode())
                        .setDisplay(entity.getCodeMaster().getCodeDisplay());
                component.setCode(compCode);
            }
            if (entity.getValueQuantity() != null) {
                String unit = entity.getValueUnit();
                if (unit == null && entity.getCodeMaster() != null) {
                    unit = entity.getCodeMaster().getExpectedUnit();
                }
                component.setValue(new Quantity()
                        .setValue(entity.getValueQuantity())
                        .setUnit(unit)
                        .setSystem("http://unitsofmeasure.org"));
            } else if (entity.getValueString() != null) {
                component.setValue(new StringType(entity.getValueString()));
            }
        }

        return observation;
    }

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
