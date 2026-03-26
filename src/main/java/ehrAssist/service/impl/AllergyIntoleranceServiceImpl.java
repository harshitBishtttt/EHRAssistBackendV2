package ehrAssist.service.impl;

import ehrAssist.entity.AllergyIntoleranceEntity;
import ehrAssist.exception.ResourceNotFoundException;
import ehrAssist.mapper.AllergyIntoleranceMapper;
import ehrAssist.repository.AllergyIntoleranceRepository;
import ehrAssist.repository.PatientRepository;
import ehrAssist.repository.PractitionerRepository;
import ehrAssist.service.AllergyIntoleranceService;
import ehrAssist.util.BundleBuilder;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Bundle;
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
public class AllergyIntoleranceServiceImpl implements AllergyIntoleranceService {

    private final AllergyIntoleranceRepository allergyIntoleranceRepository;
    private final PatientRepository patientRepository;
    private final PractitionerRepository practitionerRepository;
    private final AllergyIntoleranceMapper allergyIntoleranceMapper;
    private final BundleBuilder bundleBuilder;

    @Override
    @Transactional(readOnly = true)
    public AllergyIntolerance getById(UUID id) {
        AllergyIntoleranceEntity entity = allergyIntoleranceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AllergyIntolerance not found: " + id));
        return allergyIntoleranceMapper.toFhirResource(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Bundle search(UUID id, UUID patientId, Pageable pageable) {
        if (id == null && patientId == null) {
            return bundleBuilder.searchSetWithPagination("AllergyIntolerance", List.of(), 0L, 0, pageable.getPageSize(), "");
        }

        Specification<AllergyIntoleranceEntity> spec = Specification.where(null);
        
        if (id != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("id"), id));
        }
        if (patientId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("patient").get("id"), patientId));
        }

        Page<AllergyIntoleranceEntity> pageResult = allergyIntoleranceRepository.findAll(spec, pageable);

        List<Resource> fhirResources = pageResult.getContent().stream()
                .map(allergyIntoleranceMapper::toFhirResource)
                .map(Resource.class::cast)
                .toList();

        StringBuilder queryParams = new StringBuilder();
        if (id != null) queryParams.append("_id=").append(id).append("&");
        if (patientId != null) queryParams.append("patient=").append(patientId).append("&");
        String query = queryParams.length() > 0 ? queryParams.substring(0, queryParams.length() - 1) : "";

        return bundleBuilder.searchSetWithPagination("AllergyIntolerance", fhirResources, pageResult.getTotalElements(), 
                pageable.getPageNumber(), pageable.getPageSize(), query);
    }

    @Override
    public AllergyIntolerance create(AllergyIntolerance resource) {
        AllergyIntoleranceEntity entity = allergyIntoleranceMapper.toEntity(resource);

        if (resource.hasPatient()) {
            String ref = resource.getPatient().getReference();
            if (ref != null && ref.contains("/")) {
                UUID patientId = UUID.fromString(ref.split("/")[1]);
                entity.setPatient(patientRepository.findById(patientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId)));
            }
        }

        if (resource.hasRecorder()) {
            String ref = resource.getRecorder().getReference();
            if (ref != null && ref.contains("/")) {
                UUID practId = UUID.fromString(ref.split("/")[1]);
                entity.setRecorder(practitionerRepository.findById(practId)
                        .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found: " + practId)));
            }
        }

        AllergyIntoleranceEntity saved = allergyIntoleranceRepository.save(entity);
        return allergyIntoleranceMapper.toFhirResource(saved);
    }

    @Override
    public AllergyIntolerance update(UUID id, AllergyIntolerance resource) {
        AllergyIntoleranceEntity existing = allergyIntoleranceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AllergyIntolerance not found: " + id));

        AllergyIntoleranceEntity updated = allergyIntoleranceMapper.toEntity(resource);

        existing.setClinicalStatus(updated.getClinicalStatus());
        existing.setVerificationStatus(updated.getVerificationStatus());
        existing.setType(updated.getType());
        existing.setCategory(updated.getCategory());
        existing.setCriticality(updated.getCriticality());
        existing.setCodeSystem(updated.getCodeSystem());
        existing.setCodeValue(updated.getCodeValue());
        existing.setCodeDisplay(updated.getCodeDisplay());
        existing.setOnsetDate(updated.getOnsetDate());

        if (resource.hasPatient()) {
            String ref = resource.getPatient().getReference();
            if (ref != null && ref.contains("/")) {
                UUID patientId = UUID.fromString(ref.split("/")[1]);
                existing.setPatient(patientRepository.findById(patientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId)));
            }
        }

        if (resource.hasRecorder()) {
            String ref = resource.getRecorder().getReference();
            if (ref != null && ref.contains("/")) {
                UUID practId = UUID.fromString(ref.split("/")[1]);
                existing.setRecorder(practitionerRepository.findById(practId)
                        .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found: " + practId)));
            }
        }

        AllergyIntoleranceEntity saved = allergyIntoleranceRepository.save(existing);
        return allergyIntoleranceMapper.toFhirResource(saved);
    }

    @Override
    public void delete(UUID id) {
        AllergyIntoleranceEntity entity = allergyIntoleranceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AllergyIntolerance not found: " + id));
        allergyIntoleranceRepository.delete(entity);
    }
}
