package ehrAssist.mapper;

import ehrAssist.entity.VitalsEntity;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.Date;

@Component
public class VitalsMapper {

    public Observation toFhirResource(VitalsEntity entity) {
        Observation observation = new Observation();

        observation.setId(entity.getId().toString());

        if (!ObjectUtils.isEmpty(entity.getStatus())) {
            observation.setStatus(ObservationStatus.fromCode(entity.getStatus()));
        }

        if (!ObjectUtils.isEmpty(entity.getCodeMaster())) {
            if (!ObjectUtils.isEmpty(entity.getCodeMaster().getFhirCategoryCode())) {
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

        if (!ObjectUtils.isEmpty(entity.getPatientId())) {
            observation.setSubject(new Reference("Patient/" + entity.getPatientId()));
        }

        if (!ObjectUtils.isEmpty(entity.getEncounterId())) {
            observation.setEncounter(new Reference("Encounter/" + entity.getEncounterId()));
        }

        if (!ObjectUtils.isEmpty(entity.getEffectiveDate())) {
            observation.setEffective(new DateTimeType(toDate(entity.getEffectiveDate())));
        }

        if (!ObjectUtils.isEmpty(entity.getValueQuantity())) {
            String unit = entity.getValueUnit();
            if (ObjectUtils.isEmpty(unit) && !ObjectUtils.isEmpty(entity.getCodeMaster())) {
                unit = entity.getCodeMaster().getExpectedUnit();
            }
            observation.setValue(new Quantity()
                    .setValue(entity.getValueQuantity())
                    .setUnit(unit)
                    .setSystem("http://unitsofmeasure.org"));
        } else if (!ObjectUtils.isEmpty(entity.getValueString())) {
            observation.setValue(new StringType(entity.getValueString()));
        }

        if (!ObjectUtils.isEmpty(entity.getInterpretationCode())) {
            CodeableConcept interpretation = new CodeableConcept();
            interpretation.addCoding().setCode(entity.getInterpretationCode());
            observation.addInterpretation(interpretation);
        }

        if (!ObjectUtils.isEmpty(entity.getCodeMaster())) {
            var low = entity.getCodeMaster().getReferenceRangeLow();
            var high = entity.getCodeMaster().getReferenceRangeHigh();
            if (!ObjectUtils.isEmpty(low) || !ObjectUtils.isEmpty(high)) {
                Observation.ObservationReferenceRangeComponent range = observation.addReferenceRange();
                if (!ObjectUtils.isEmpty(low)) range.setLow(new Quantity().setValue(low));
                if (!ObjectUtils.isEmpty(high)) range.setHigh(new Quantity().setValue(high));
            }
        }

        return observation;
    }

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
