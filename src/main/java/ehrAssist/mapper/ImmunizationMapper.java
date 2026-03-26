package ehrAssist.mapper;

import ehrAssist.entity.ImmunizationEntity;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Immunization.ImmunizationStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class ImmunizationMapper {

    public Immunization toFhirResource(ImmunizationEntity entity) {
        Immunization immunization = new Immunization();

        immunization.setId(entity.getId().toString());

        Meta meta = new Meta();
        if (entity.getVersion() != null) {
            meta.setVersionId(entity.getVersion().toString());
        }
        if (entity.getUpdatedAt() != null) {
            meta.setLastUpdated(toDate(entity.getUpdatedAt()));
        }
        immunization.setMeta(meta);

        if (entity.getStatus() != null) {
            immunization.setStatus(ImmunizationStatus.fromCode(entity.getStatus()));
        }

        if (entity.getVaccineCodeSystem() != null || entity.getVaccineCodeValue() != null) {
            CodeableConcept vaccineCode = new CodeableConcept();
            vaccineCode.addCoding()
                    .setSystem(entity.getVaccineCodeSystem())
                    .setCode(entity.getVaccineCodeValue())
                    .setDisplay(entity.getVaccineCodeDisplay());
            immunization.setVaccineCode(vaccineCode);
        }

        if (entity.getPatient() != null) {
            immunization.setPatient(new Reference("Patient/" + entity.getPatient().getId()));
        }

        if (entity.getEncounter() != null) {
            immunization.setEncounter(new Reference("Encounter/" + entity.getEncounter().getId()));
        }

        if (entity.getPerformer() != null) {
            Immunization.ImmunizationPerformerComponent performer = immunization.addPerformer();
            performer.setActor(new Reference("Practitioner/" + entity.getPerformer().getId()));
        }

        if (entity.getOccurrenceDate() != null) {
            immunization.setOccurrence(new DateTimeType(toDate(entity.getOccurrenceDate())));
        }

        if (entity.getLotNumber() != null) {
            immunization.setLotNumber(entity.getLotNumber());
        }

        if (entity.getSiteCode() != null) {
            CodeableConcept site = new CodeableConcept();
            site.addCoding().setCode(entity.getSiteCode());
            immunization.setSite(site);
        }

        if (entity.getRouteCode() != null) {
            CodeableConcept route = new CodeableConcept();
            route.addCoding().setCode(entity.getRouteCode());
            immunization.setRoute(route);
        }

        if (entity.getDoseQuantity() != null) {
            immunization.setDoseQuantity(new Quantity().setValue(entity.getDoseQuantity()));
        }

        return immunization;
    }

    public ImmunizationEntity toEntity(Immunization fhir) {
        ImmunizationEntity entity = new ImmunizationEntity();

        if (fhir.hasStatus()) {
            entity.setStatus(fhir.getStatus().toCode());
        }

        if (fhir.hasVaccineCode() && fhir.getVaccineCode().hasCoding()) {
            Coding coding = fhir.getVaccineCode().getCodingFirstRep();
            entity.setVaccineCodeSystem(coding.getSystem());
            entity.setVaccineCodeValue(coding.getCode());
            entity.setVaccineCodeDisplay(coding.getDisplay());
        }

        if (fhir.hasOccurrence() && fhir.getOccurrence() instanceof DateTimeType) {
            entity.setOccurrenceDate(
                    toLocalDateTime(((DateTimeType) fhir.getOccurrence()).getValue()));
        }

        if (fhir.hasLotNumber()) {
            entity.setLotNumber(fhir.getLotNumber());
        }

        if (fhir.hasSite() && fhir.getSite().hasCoding()) {
            entity.setSiteCode(fhir.getSite().getCodingFirstRep().getCode());
        }

        if (fhir.hasRoute() && fhir.getRoute().hasCoding()) {
            entity.setRouteCode(fhir.getRoute().getCodingFirstRep().getCode());
        }

        if (fhir.hasDoseQuantity()) {
            entity.setDoseQuantity(fhir.getDoseQuantity().getValue());
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
