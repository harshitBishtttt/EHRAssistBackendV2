package ehrAssist.mapper;

import ehrAssist.entity.AllergyIntoleranceEntity;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.AllergyIntolerance.AllergyIntoleranceType;
import org.hl7.fhir.r4.model.AllergyIntolerance.AllergyIntoleranceCategory;
import org.hl7.fhir.r4.model.AllergyIntolerance.AllergyIntoleranceCriticality;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class AllergyIntoleranceMapper {

    public AllergyIntolerance toFhirResource(AllergyIntoleranceEntity entity) {
        AllergyIntolerance allergy = new AllergyIntolerance();

        allergy.setId(entity.getId().toString());

        Meta meta = new Meta();
        if (entity.getVersion() != null) {
            meta.setVersionId(entity.getVersion().toString());
        }
        if (entity.getUpdatedAt() != null) {
            meta.setLastUpdated(toDate(entity.getUpdatedAt()));
        }
        allergy.setMeta(meta);

        if (entity.getClinicalStatus() != null) {
            CodeableConcept clinicalStatus = new CodeableConcept();
            clinicalStatus.addCoding()
                    .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical")
                    .setCode(entity.getClinicalStatus());
            allergy.setClinicalStatus(clinicalStatus);
        }

        if (entity.getVerificationStatus() != null) {
            CodeableConcept verificationStatus = new CodeableConcept();
            verificationStatus.addCoding()
                    .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-verification")
                    .setCode(entity.getVerificationStatus());
            allergy.setVerificationStatus(verificationStatus);
        }

        if (entity.getType() != null) {
            allergy.setType(AllergyIntoleranceType.fromCode(entity.getType()));
        }

        if (entity.getCategory() != null) {
            allergy.addCategory(AllergyIntoleranceCategory.fromCode(entity.getCategory()));
        }

        if (entity.getCriticality() != null) {
            allergy.setCriticality(AllergyIntoleranceCriticality.fromCode(entity.getCriticality()));
        }

        if (entity.getCodeSystem() != null || entity.getCodeValue() != null) {
            CodeableConcept code = new CodeableConcept();
            code.addCoding()
                    .setSystem(entity.getCodeSystem())
                    .setCode(entity.getCodeValue())
                    .setDisplay(entity.getCodeDisplay());
            allergy.setCode(code);
        }

        if (entity.getPatient() != null) {
            allergy.setPatient(new Reference("Patient/" + entity.getPatient().getId()));
        }

        if (entity.getRecorder() != null) {
            allergy.setRecorder(new Reference("Practitioner/" + entity.getRecorder().getId()));
        }

        if (entity.getOnsetDate() != null) {
            allergy.setOnset(new DateTimeType(toDate(entity.getOnsetDate())));
        }

        return allergy;
    }

    public AllergyIntoleranceEntity toEntity(AllergyIntolerance fhir) {
        AllergyIntoleranceEntity entity = new AllergyIntoleranceEntity();

        if (fhir.hasClinicalStatus() && fhir.getClinicalStatus().hasCoding()) {
            entity.setClinicalStatus(fhir.getClinicalStatus().getCodingFirstRep().getCode());
        }

        if (fhir.hasVerificationStatus() && fhir.getVerificationStatus().hasCoding()) {
            entity.setVerificationStatus(fhir.getVerificationStatus().getCodingFirstRep().getCode());
        }

        if (fhir.hasType()) {
            entity.setType(fhir.getType().toCode());
        }

        if (fhir.hasCategory()) {
            entity.setCategory(fhir.getCategory().get(0).getValue().toCode());
        }

        if (fhir.hasCriticality()) {
            entity.setCriticality(fhir.getCriticality().toCode());
        }

        if (fhir.hasCode() && fhir.getCode().hasCoding()) {
            Coding coding = fhir.getCode().getCodingFirstRep();
            entity.setCodeSystem(coding.getSystem());
            entity.setCodeValue(coding.getCode());
            entity.setCodeDisplay(coding.getDisplay());
        }

        if (fhir.hasOnset() && fhir.getOnset() instanceof DateTimeType) {
            entity.setOnsetDate(toLocalDate(((DateTimeType) fhir.getOnset()).getValue()));
        }

        return entity;
    }

    private Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
