package ehrAssist.mapper;

import ehrAssist.entity.PractitionerEntity;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class PractitionerMapper {

    public Practitioner toFhirResource(PractitionerEntity entity) {
        Practitioner practitioner = new Practitioner();

        practitioner.setId(entity.getId().toString());

        Meta meta = new Meta();
        if (entity.getVersion() != null) {
            meta.setVersionId(entity.getVersion().toString());
        }
        if (entity.getUpdatedAt() != null) {
            meta.setLastUpdated(toDate(entity.getUpdatedAt()));
        }
        practitioner.setMeta(meta);

        if (entity.getActive() != null) {
            practitioner.setActive(entity.getActive());
        }

        HumanName name = practitioner.addName();
        name.setFamily(entity.getFamilyName());
        if (entity.getGivenName() != null) {
            name.addGiven(entity.getGivenName());
        }

        if (entity.getGender() != null) {
            practitioner.setGender(AdministrativeGender.fromCode(entity.getGender()));
        }

        if (entity.getBirthDate() != null) {
            practitioner.setBirthDate(toDate(entity.getBirthDate()));
        }

        if (entity.getNpi() != null) {
            practitioner.addIdentifier()
                    .setSystem("http://hl7.org/fhir/sid/us-npi")
                    .setValue(entity.getNpi());
        }

        if (entity.getSpecialtyCode() != null) {
            Practitioner.PractitionerQualificationComponent qualification =
                    practitioner.addQualification();
            CodeableConcept code = new CodeableConcept();
            code.addCoding()
                    .setCode(entity.getSpecialtyCode())
                    .setDisplay(entity.getSpecialtyDisplay());
            qualification.setCode(code);
        }

        if (entity.getPhone() != null) {
            practitioner.addTelecom()
                    .setSystem(ContactPoint.ContactPointSystem.PHONE)
                    .setValue(entity.getPhone())
                    .setUse(ContactPoint.ContactPointUse.WORK);
        }

        if (entity.getEmail() != null) {
            practitioner.addTelecom()
                    .setSystem(ContactPoint.ContactPointSystem.EMAIL)
                    .setValue(entity.getEmail())
                    .setUse(ContactPoint.ContactPointUse.WORK);
        }

        return practitioner;
    }

    public PractitionerEntity toEntity(Practitioner fhir) {
        PractitionerEntity entity = new PractitionerEntity();

        if (fhir.hasActive()) {
            entity.setActive(fhir.getActive());
        }

        if (fhir.hasName()) {
            HumanName name = fhir.getNameFirstRep();
            entity.setFamilyName(name.getFamily());
            if (name.hasGiven()) {
                entity.setGivenName(name.getGiven().get(0).getValue());
            }
        }

        if (fhir.hasGender()) {
            entity.setGender(fhir.getGender().toCode());
        }

        if (fhir.hasBirthDate()) {
            entity.setBirthDate(toLocalDate(fhir.getBirthDate()));
        }

        if (fhir.hasIdentifier()) {
            for (Identifier id : fhir.getIdentifier()) {
                if ("http://hl7.org/fhir/sid/us-npi".equals(id.getSystem())) {
                    entity.setNpi(id.getValue());
                    break;
                }
            }
        }

        if (fhir.hasQualification()) {
            CodeableConcept code = fhir.getQualificationFirstRep().getCode();
            if (code != null && code.hasCoding()) {
                entity.setSpecialtyCode(code.getCodingFirstRep().getCode());
                entity.setSpecialtyDisplay(code.getCodingFirstRep().getDisplay());
            }
        }

        for (ContactPoint telecom : fhir.getTelecom()) {
            if (telecom.hasSystem()) {
                if (telecom.getSystem() == ContactPoint.ContactPointSystem.PHONE) {
                    entity.setPhone(telecom.getValue());
                } else if (telecom.getSystem() == ContactPoint.ContactPointSystem.EMAIL) {
                    entity.setEmail(telecom.getValue());
                }
            }
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
