package ehrAssist.service.impl;

import ehrAssist.entity.ServiceRequestEntity;
import ehrAssist.exception.ResourceNotFoundException;
import ehrAssist.mapper.ServiceRequestMapper;
import ehrAssist.repository.EncounterRepository;
import ehrAssist.repository.PatientRepository;
import ehrAssist.repository.PractitionerRepository;
import ehrAssist.repository.ServiceRequestRepository;
import ehrAssist.service.ServiceRequestService;
import ehrAssist.util.BundleBuilder;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
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
public class ServiceRequestServiceImpl implements ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final PractitionerRepository practitionerRepository;
    private final ServiceRequestMapper serviceRequestMapper;
    private final BundleBuilder bundleBuilder;

    @Override
    @Transactional(readOnly = true)
    public ServiceRequest getById(UUID id) {
        ServiceRequestEntity entity = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest not found: " + id));
        return serviceRequestMapper.toFhirResource(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Bundle search(UUID id, UUID patientId, Pageable pageable) {
        if (id == null && patientId == null) {
            return bundleBuilder.searchSetWithPagination("ServiceRequest", List.of(), 0L, 0, pageable.getPageSize(), "");
        }

        Specification<ServiceRequestEntity> spec = Specification.where(null);
        
        if (id != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("id"), id));
        }
        if (patientId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("patient").get("id"), patientId));
        }

        Page<ServiceRequestEntity> pageResult = serviceRequestRepository.findAll(spec, pageable);

        List<Resource> fhirResources = pageResult.getContent().stream()
                .map(serviceRequestMapper::toFhirResource)
                .map(Resource.class::cast)
                .toList();

        StringBuilder queryParams = new StringBuilder();
        if (id != null) queryParams.append("_id=").append(id).append("&");
        if (patientId != null) queryParams.append("patient=").append(patientId).append("&");
        String query = queryParams.length() > 0 ? queryParams.substring(0, queryParams.length() - 1) : "";

        return bundleBuilder.searchSetWithPagination("ServiceRequest", fhirResources, pageResult.getTotalElements(), 
                pageable.getPageNumber(), pageable.getPageSize(), query);
    }

    @Override
    public ServiceRequest create(ServiceRequest resource) {
        ServiceRequestEntity entity = serviceRequestMapper.toEntity(resource);

        if (resource.hasSubject()) {
            String ref = resource.getSubject().getReference();
            if (ref != null && ref.contains("/")) {
                UUID patientId = UUID.fromString(ref.split("/")[1]);
                entity.setPatient(patientRepository.findById(patientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId)));
            }
        }

        if (resource.hasEncounter()) {
            String ref = resource.getEncounter().getReference();
            if (ref != null && ref.contains("/")) {
                UUID encId = UUID.fromString(ref.split("/")[1]);
                entity.setEncounter(encounterRepository.findById(encId)
                        .orElseThrow(() -> new ResourceNotFoundException("Encounter not found: " + encId)));
            }
        }

        if (resource.hasRequester()) {
            String ref = resource.getRequester().getReference();
            if (ref != null && ref.contains("/")) {
                UUID practId = UUID.fromString(ref.split("/")[1]);
                entity.setRequester(practitionerRepository.findById(practId)
                        .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found: " + practId)));
            }
        }

        ServiceRequestEntity saved = serviceRequestRepository.save(entity);
        return serviceRequestMapper.toFhirResource(saved);
    }

    @Override
    public ServiceRequest update(UUID id, ServiceRequest resource) {
        ServiceRequestEntity existing = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest not found: " + id));

        ServiceRequestEntity updated = serviceRequestMapper.toEntity(resource);

        existing.setStatus(updated.getStatus());
        existing.setIntent(updated.getIntent());
        existing.setCategoryCode(updated.getCategoryCode());
        existing.setCodeSystem(updated.getCodeSystem());
        existing.setCodeValue(updated.getCodeValue());
        existing.setCodeDisplay(updated.getCodeDisplay());
        existing.setAuthoredOn(updated.getAuthoredOn());
        existing.setNote(updated.getNote());

        if (resource.hasSubject()) {
            String ref = resource.getSubject().getReference();
            if (ref != null && ref.contains("/")) {
                UUID patientId = UUID.fromString(ref.split("/")[1]);
                existing.setPatient(patientRepository.findById(patientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId)));
            }
        }

        if (resource.hasEncounter()) {
            String ref = resource.getEncounter().getReference();
            if (ref != null && ref.contains("/")) {
                UUID encId = UUID.fromString(ref.split("/")[1]);
                existing.setEncounter(encounterRepository.findById(encId)
                        .orElseThrow(() -> new ResourceNotFoundException("Encounter not found: " + encId)));
            }
        }

        if (resource.hasRequester()) {
            String ref = resource.getRequester().getReference();
            if (ref != null && ref.contains("/")) {
                UUID practId = UUID.fromString(ref.split("/")[1]);
                existing.setRequester(practitionerRepository.findById(practId)
                        .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found: " + practId)));
            }
        }

        ServiceRequestEntity saved = serviceRequestRepository.save(existing);
        return serviceRequestMapper.toFhirResource(saved);
    }

    @Override
    public void delete(UUID id) {
        ServiceRequestEntity entity = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest not found: " + id));
        serviceRequestRepository.delete(entity);
    }
}
