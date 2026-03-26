package ehrAssist.mapper;

import ehrAssist.entity.ObservationEntity;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class ObservationMapper {

    public Observation toFhirResource(ObservationEntity entity) {
        Observation observation = new Observation();

        observation.setId(entity.getId().toString());

        Meta meta = new Meta();
        if (entity.getVersion() != null) {
            meta.setVersionId(entity.getVersion().toString());
        }
        if (entity.getUpdatedAt() != null) {
            meta.setLastUpdated(toDate(entity.getUpdatedAt()));
        }
        observation.setMeta(meta);

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
            observation.setCode(code);
        }

        if (entity.getPatient() != null) {
            observation.setSubject(new Reference("Patient/" + entity.getPatient().getId()));
        }

        if (entity.getEncounter() != null) {
            observation.setEncounter(new Reference("Encounter/" + entity.getEncounter().getId()));
        }

        if (entity.getPerformer() != null) {
            observation.addPerformer(new Reference("Practitioner/" + entity.getPerformer().getId()));
        }

        if (entity.getEffectiveDate() != null) {
            observation.setEffective(new DateTimeType(toDate(entity.getEffectiveDate())));
        }

        if (entity.getIssued() != null) {
            observation.setIssued(toDate(entity.getIssued()));
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
                Observation.ObservationReferenceRangeComponent range =
                        observation.addReferenceRange();
                if (low != null) {
                    range.setLow(new Quantity().setValue(low));
                }
                if (high != null) {
                    range.setHigh(new Quantity().setValue(high));
                }
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

    public ObservationEntity toEntity(Observation fhir) {
        ObservationEntity entity = new ObservationEntity();

        if (fhir.hasStatus()) {
            entity.setStatus(fhir.getStatus().toCode());
        }

        if (fhir.hasEffectiveDateTimeType()) {
            entity.setEffectiveDate(toLocalDateTime(fhir.getEffectiveDateTimeType().getValue()));
        }

        if (fhir.hasIssued()) {
            entity.setIssued(toLocalDateTime(fhir.getIssued()));
        }

        if (fhir.hasValue()) {
            if (fhir.getValue() instanceof Quantity) {
                Quantity qty = (Quantity) fhir.getValue();
                entity.setValueQuantity(qty.getValue());
                entity.setValueUnit(qty.getUnit());
            } else if (fhir.getValue() instanceof StringType) {
                entity.setValueString(((StringType) fhir.getValue()).getValue());
            }
        }

        if (fhir.hasInterpretation() && !fhir.getInterpretation().isEmpty()) {
            CodeableConcept interp = fhir.getInterpretationFirstRep();
            if (interp.hasCoding()) {
                entity.setInterpretationCode(interp.getCodingFirstRep().getCode());
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
