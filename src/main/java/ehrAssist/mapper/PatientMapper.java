package ehrAssist.mapper;

import ehrAssist.entity.*;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;

@Component
public class PatientMapper {

    public Patient toFhirResource(PatientEntity entity) {
        Patient patient = new Patient();

        patient.setId(entity.getId().toString());

        Meta meta = new Meta();
        if (entity.getVersion() != null) {
            meta.setVersionId(entity.getVersion().toString());
        }
        if (entity.getUpdatedAt() != null) {
            meta.setLastUpdated(toDate(entity.getUpdatedAt()));
        }
        patient.setMeta(meta);

        if (entity.getActive() != null) {
            patient.setActive(entity.getActive());
        }

        if (entity.getGender() != null) {
            patient.setGender(AdministrativeGender.fromCode(entity.getGender()));
        }

        if (entity.getBirthDate() != null) {
            patient.setBirthDate(toDate(entity.getBirthDate()));
        }

        if (entity.getDeceasedDate() != null) {
            patient.setDeceased(new DateTimeType(toDate(entity.getDeceasedDate())));
        } else if (Boolean.TRUE.equals(entity.getDeceasedFlag())) {
            patient.setDeceased(new BooleanType(true));
        }

        if (entity.getNames() != null) {
            for (PatientNameEntity nameEntity : entity.getNames()) {
                HumanName name = patient.addName();
                if (nameEntity.getUseType() != null) {
                    name.setUse(HumanName.NameUse.fromCode(nameEntity.getUseType()));
                }
                name.setFamily(nameEntity.getFamily());
                if (nameEntity.getGivenFirst() != null) {
                    name.addGiven(nameEntity.getGivenFirst());
                }
                if (nameEntity.getGivenMiddle() != null) {
                    name.addGiven(nameEntity.getGivenMiddle());
                }
                if (nameEntity.getPrefix() != null) {
                    name.addPrefix(nameEntity.getPrefix());
                }
                if (nameEntity.getSuffix() != null) {
                    name.addSuffix(nameEntity.getSuffix());
                }
                if (nameEntity.getPeriodStart() != null || nameEntity.getPeriodEnd() != null) {
                    Period period = new Period();
                    if (nameEntity.getPeriodStart() != null) {
                        period.setStart(toDate(nameEntity.getPeriodStart()));
                    }
                    if (nameEntity.getPeriodEnd() != null) {
                        period.setEnd(toDate(nameEntity.getPeriodEnd()));
                    }
                    name.setPeriod(period);
                }
            }
        }

        if (entity.getTelecoms() != null) {
            for (PatientTelecomEntity telecomEntity : entity.getTelecoms()) {
                ContactPoint telecom = patient.addTelecom();
                if (telecomEntity.getSystem() != null) {
                    telecom.setSystem(ContactPoint.ContactPointSystem.fromCode(telecomEntity.getSystem()));
                }
                telecom.setValue(telecomEntity.getValue());
                if (telecomEntity.getUseType() != null) {
                    telecom.setUse(ContactPoint.ContactPointUse.fromCode(telecomEntity.getUseType()));
                }
            }
        }

        if (entity.getAddresses() != null) {
            for (PatientAddressEntity addrEntity : entity.getAddresses()) {
                Address address = patient.addAddress();
                if (addrEntity.getUseType() != null) {
                    address.setUse(Address.AddressUse.fromCode(addrEntity.getUseType()));
                }
                if (addrEntity.getLine1() != null) {
                    address.addLine(addrEntity.getLine1());
                }
                if (addrEntity.getLine2() != null) {
                    address.addLine(addrEntity.getLine2());
                }
                address.setCity(addrEntity.getCity());
                address.setState(addrEntity.getState());
                address.setPostalCode(addrEntity.getPostalCode());
                address.setCountry(addrEntity.getCountry());
            }
        }

        if (entity.getIdentifiers() != null) {
            for (PatientIdentifierEntity idEntity : entity.getIdentifiers()) {
                Identifier identifier = patient.addIdentifier();
                identifier.setSystem(idEntity.getSystem());
                identifier.setValue(idEntity.getValue());
                if (idEntity.getTypeCode() != null) {
                    identifier.setType(new CodeableConcept().addCoding(
                            new Coding().setCode(idEntity.getTypeCode())
                    ));
                }
            }
        }

        if (entity.getMaritalStatusCode() != null) {
            CodeableConcept maritalStatus = new CodeableConcept();
            maritalStatus.addCoding()
                    .setSystem("http://terminology.hl7.org/CodeSystem/v3-MaritalStatus")
                    .setCode(entity.getMaritalStatusCode())
                    .setDisplay(entity.getMaritalStatusDisplay());
            patient.setMaritalStatus(maritalStatus);
        }

        if (entity.getLanguageCode() != null) {
            CodeableConcept language = new CodeableConcept();
            language.addCoding()
                    .setSystem("urn:ietf:bcp:47")
                    .setCode(entity.getLanguageCode())
                    .setDisplay(entity.getLanguageDisplay());
            patient.addCommunication().setLanguage(language);
        }

        if (entity.getPrimaryPractitioner() != null) {
            patient.addGeneralPractitioner(
                    new Reference("Practitioner/" + entity.getPrimaryPractitioner().getId()));
        }

        if (entity.getManagingOrganization() != null) {
            patient.setManagingOrganization(
                    new Reference("Organization/" + entity.getManagingOrganization().getId()));
        }

        return patient;
    }

    public PatientEntity toEntity(Patient fhir) {
        PatientEntity entity = new PatientEntity();

        if (fhir.hasActive()) {
            entity.setActive(fhir.getActive());
        }

        if (fhir.hasGender()) {
            entity.setGender(fhir.getGender().toCode());
        }

        if (fhir.hasBirthDate()) {
            entity.setBirthDate(toLocalDate(fhir.getBirthDate()));
        }

        if (fhir.hasDeceased()) {
            if (fhir.getDeceased() instanceof DateTimeType) {
                entity.setDeceasedDate(toLocalDateTime(((DateTimeType) fhir.getDeceased()).getValue()));
                entity.setDeceasedFlag(true);
            } else if (fhir.getDeceased() instanceof BooleanType) {
                entity.setDeceasedFlag(((BooleanType) fhir.getDeceased()).booleanValue());
            }
        }

        if (fhir.hasMaritalStatus() && fhir.getMaritalStatus().hasCoding()) {
            Coding coding = fhir.getMaritalStatus().getCodingFirstRep();
            entity.setMaritalStatusCode(coding.getCode());
            entity.setMaritalStatusDisplay(coding.getDisplay());
        }

        if (fhir.hasCommunication() && !fhir.getCommunication().isEmpty()) {
            CodeableConcept lang = fhir.getCommunicationFirstRep().getLanguage();
            if (lang != null && lang.hasCoding()) {
                entity.setLanguageCode(lang.getCodingFirstRep().getCode());
                entity.setLanguageDisplay(lang.getCodingFirstRep().getDisplay());
            }
        }

        entity.setNames(new ArrayList<>());
        for (HumanName name : fhir.getName()) {
            PatientNameEntity nameEntity = new PatientNameEntity();
            nameEntity.setPatient(entity);
            if (name.hasUse()) {
                nameEntity.setUseType(name.getUse().toCode());
            }
            nameEntity.setFamily(name.getFamily());
            if (name.hasGiven()) {
                nameEntity.setGivenFirst(name.getGiven().get(0).getValue());
                if (name.getGiven().size() > 1) {
                    nameEntity.setGivenMiddle(name.getGiven().get(1).getValue());
                }
            }
            if (name.hasPrefix()) {
                nameEntity.setPrefix(name.getPrefix().get(0).getValue());
            }
            if (name.hasSuffix()) {
                nameEntity.setSuffix(name.getSuffix().get(0).getValue());
            }
            if (name.hasPeriod()) {
                if (name.getPeriod().hasStart()) {
                    nameEntity.setPeriodStart(toLocalDate(name.getPeriod().getStart()));
                }
                if (name.getPeriod().hasEnd()) {
                    nameEntity.setPeriodEnd(toLocalDate(name.getPeriod().getEnd()));
                }
            }
            entity.getNames().add(nameEntity);
        }

        entity.setAddresses(new ArrayList<>());
        for (Address address : fhir.getAddress()) {
            PatientAddressEntity addrEntity = new PatientAddressEntity();
            addrEntity.setPatient(entity);
            if (address.hasUse()) {
                addrEntity.setUseType(address.getUse().toCode());
            }
            if (address.hasLine()) {
                addrEntity.setLine1(address.getLine().get(0).getValue());
                if (address.getLine().size() > 1) {
                    addrEntity.setLine2(address.getLine().get(1).getValue());
                }
            }
            addrEntity.setCity(address.getCity());
            addrEntity.setState(address.getState());
            addrEntity.setPostalCode(address.getPostalCode());
            addrEntity.setCountry(address.getCountry());
            entity.getAddresses().add(addrEntity);
        }

        entity.setTelecoms(new ArrayList<>());
        for (ContactPoint telecom : fhir.getTelecom()) {
            PatientTelecomEntity telecomEntity = new PatientTelecomEntity();
            telecomEntity.setPatient(entity);
            if (telecom.hasSystem()) {
                telecomEntity.setSystem(telecom.getSystem().toCode());
            }
            telecomEntity.setValue(telecom.getValue());
            if (telecom.hasUse()) {
                telecomEntity.setUseType(telecom.getUse().toCode());
            }
            entity.getTelecoms().add(telecomEntity);
        }

        entity.setIdentifiers(new ArrayList<>());
        for (Identifier identifier : fhir.getIdentifier()) {
            PatientIdentifierEntity idEntity = new PatientIdentifierEntity();
            idEntity.setPatient(entity);
            idEntity.setSystem(identifier.getSystem());
            idEntity.setValue(identifier.getValue());
            if (identifier.hasType() && identifier.getType().hasCoding()) {
                idEntity.setTypeCode(identifier.getType().getCodingFirstRep().getCode());
            }
            entity.getIdentifiers().add(idEntity);
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

    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
