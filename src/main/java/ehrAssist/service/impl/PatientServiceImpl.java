package ehrAssist.service.impl;

import ehrAssist.entity.PatientAddressEntity;
import ehrAssist.entity.PatientEntity;
import ehrAssist.entity.PatientIdentifierEntity;
import ehrAssist.entity.PatientNameEntity;
import ehrAssist.entity.PatientTelecomEntity;
import ehrAssist.exception.ResourceNotFoundException;
import ehrAssist.mapper.PatientMapper;
import ehrAssist.repository.OrganizationRepository;
import ehrAssist.repository.PatientRepository;
import ehrAssist.repository.PractitionerRepository;
import ehrAssist.service.PatientService;
import ehrAssist.util.BundleBuilder;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PractitionerRepository practitionerRepository;
    private final OrganizationRepository organizationRepository;
    private final PatientMapper patientMapper;
    private final BundleBuilder bundleBuilder;

    @Override
    @Transactional(readOnly = true)
    public Patient getById(UUID id) {
        PatientEntity entity = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + id));
        return patientMapper.toFhirResource(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Bundle search(UUID id, String family, String given, String gender, String birthdate, String email, Pageable pageable) {
        // If no parameters provided, return empty bundle
        if (id == null && family == null && given == null && gender == null && birthdate == null && email == null) {
            return bundleBuilder.searchSetWithPagination("Patient", List.of(), 0L, 0, pageable.getPageSize(), "");
        }

        // Build specification with all provided parameters (including _id)
        Specification<PatientEntity> spec = Specification.where(null);

        if (id != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("id"), id));
        }
        if (family != null) {
            spec = spec.and((root, query, cb) -> {
                var nameJoin = root.join("names");
                return cb.equal(cb.lower(nameJoin.get("family")), family.toLowerCase());
            });
        }
        if (given != null) {
            spec = spec.and((root, query, cb) -> {
                var nameJoin = root.join("names");
                return cb.equal(cb.lower(nameJoin.get("givenFirst")), given.toLowerCase());
            });
        }
        if (gender != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("gender"), gender));
        }
        if (birthdate != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("birthDate").as(String.class), birthdate));
        }
        if (email != null) {
            spec = spec.and((root, query, cb) -> {
                var telecomJoin = root.join("telecoms");
                return cb.and(
                        cb.equal(telecomJoin.get("system"), "email"),
                        cb.equal(cb.lower(telecomJoin.get("value")), email.toLowerCase())
                );
            });
        }

        Page<PatientEntity> pageResult = patientRepository.findAll(spec, pageable);

        List<Resource> fhirResources = pageResult.getContent().stream()
                .map(patientMapper::toFhirResource)
                .map(Resource.class::cast)
                .toList();

        StringBuilder queryParams = new StringBuilder();
        if (id != null) queryParams.append("_id=").append(id).append("&");
        if (family != null) queryParams.append("family=").append(family).append("&");
        if (given != null) queryParams.append("given=").append(given).append("&");
        if (gender != null) queryParams.append("gender=").append(gender).append("&");
        if (birthdate != null) queryParams.append("birthdate=").append(birthdate).append("&");
        if (email != null) queryParams.append("email=").append(email).append("&");
        String query = queryParams.length() > 0 ? queryParams.substring(0, queryParams.length() - 1) : "";

        return bundleBuilder.searchSetWithPagination("Patient", fhirResources, pageResult.getTotalElements(), 
                pageable.getPageNumber(), pageable.getPageSize(), query);
    }

    @Override
    public Patient create(Patient resource) {
        PatientEntity entity = patientMapper.toEntity(resource);

        if (resource.hasGeneralPractitioner()) {
            String ref = resource.getGeneralPractitionerFirstRep().getReference();
            if (ref != null && ref.contains("/")) {
                UUID practId = UUID.fromString(ref.split("/")[1]);
                entity.setPrimaryPractitioner(practitionerRepository.findById(practId)
                        .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found: " + practId)));
            }
        }

        if (resource.hasManagingOrganization()) {
            String ref = resource.getManagingOrganization().getReference();
            if (ref != null && ref.contains("/")) {
                UUID orgId = UUID.fromString(ref.split("/")[1]);
                entity.setManagingOrganization(organizationRepository.findById(orgId)
                        .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + orgId)));
            }
        }

        entity.getNames().forEach(n -> n.setPatient(entity));
        entity.getAddresses().forEach(a -> a.setPatient(entity));
        entity.getTelecoms().forEach(t -> t.setPatient(entity));
        entity.getIdentifiers().forEach(i -> i.setPatient(entity));

        PatientEntity saved = patientRepository.save(entity);
        return patientMapper.toFhirResource(saved);
    }

    @Override
    public Patient update(UUID id, Patient resource) {
        PatientEntity existing = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + id));

        PatientEntity updated = patientMapper.toEntity(resource);

        existing.setActive(updated.getActive());
        existing.setGender(updated.getGender());
        existing.setBirthDate(updated.getBirthDate());
        existing.setDeceasedFlag(updated.getDeceasedFlag());
        existing.setDeceasedDate(updated.getDeceasedDate());
        existing.setMaritalStatusCode(updated.getMaritalStatusCode());
        existing.setMaritalStatusDisplay(updated.getMaritalStatusDisplay());
        existing.setLanguageCode(updated.getLanguageCode());
        existing.setLanguageDisplay(updated.getLanguageDisplay());

        existing.getNames().clear();
        for (PatientNameEntity name : updated.getNames()) {
            name.setPatient(existing);
            existing.getNames().add(name);
        }

        existing.getAddresses().clear();
        for (PatientAddressEntity addr : updated.getAddresses()) {
            addr.setPatient(existing);
            existing.getAddresses().add(addr);
        }

        existing.getTelecoms().clear();
        for (PatientTelecomEntity telecom : updated.getTelecoms()) {
            telecom.setPatient(existing);
            existing.getTelecoms().add(telecom);
        }

        existing.getIdentifiers().clear();
        for (PatientIdentifierEntity identifier : updated.getIdentifiers()) {
            identifier.setPatient(existing);
            existing.getIdentifiers().add(identifier);
        }

        if (resource.hasGeneralPractitioner()) {
            String ref = resource.getGeneralPractitionerFirstRep().getReference();
            if (ref != null && ref.contains("/")) {
                UUID practId = UUID.fromString(ref.split("/")[1]);
                existing.setPrimaryPractitioner(practitionerRepository.findById(practId)
                        .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found: " + practId)));
            }
        }

        if (resource.hasManagingOrganization()) {
            String ref = resource.getManagingOrganization().getReference();
            if (ref != null && ref.contains("/")) {
                UUID orgId = UUID.fromString(ref.split("/")[1]);
                existing.setManagingOrganization(organizationRepository.findById(orgId)
                        .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + orgId)));
            }
        }

        PatientEntity saved = patientRepository.save(existing);
        return patientMapper.toFhirResource(saved);
    }

    @Override
    public void delete(UUID id) {
        PatientEntity entity = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + id));
        patientRepository.delete(entity);
    }
}
