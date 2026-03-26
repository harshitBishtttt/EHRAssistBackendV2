package ehrAssist.service.impl;

import ehrAssist.entity.ImmunizationEntity;
import ehrAssist.exception.ResourceNotFoundException;
import ehrAssist.mapper.ImmunizationMapper;
import ehrAssist.repository.EncounterRepository;
import ehrAssist.repository.ImmunizationRepository;
import ehrAssist.repository.PatientRepository;
import ehrAssist.repository.PractitionerRepository;
import ehrAssist.service.ImmunizationService;
import ehrAssist.util.BundleBuilder;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Immunization;
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
public class ImmunizationServiceImpl implements ImmunizationService {

    private final ImmunizationRepository immunizationRepository;
    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final PractitionerRepository practitionerRepository;
    private final ImmunizationMapper immunizationMapper;
    private final BundleBuilder bundleBuilder;

    @Override
    @Transactional(readOnly = true)
    public Immunization getById(UUID id) {
        ImmunizationEntity entity = immunizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Immunization not found: " + id));
        return immunizationMapper.toFhirResource(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Bundle search(UUID id, UUID patientId, Pageable pageable) {
        if (id == null && patientId == null) {
            return bundleBuilder.searchSetWithPagination("Immunization", List.of(), 0L, 0, pageable.getPageSize(), "");
        }

        Specification<ImmunizationEntity> spec = Specification.where(null);
        
        if (id != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("id"), id));
        }
        if (patientId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("patient").get("id"), patientId));
        }

        Page<ImmunizationEntity> pageResult = immunizationRepository.findAll(spec, pageable);

        List<Resource> fhirResources = pageResult.getContent().stream()
                .map(immunizationMapper::toFhirResource)
                .map(Resource.class::cast)
                .toList();

        StringBuilder queryParams = new StringBuilder();
        if (id != null) queryParams.append("_id=").append(id).append("&");
        if (patientId != null) queryParams.append("patient=").append(patientId).append("&");
        String query = queryParams.length() > 0 ? queryParams.substring(0, queryParams.length() - 1) : "";

        return bundleBuilder.searchSetWithPagination("Immunization", fhirResources, pageResult.getTotalElements(), 
                pageable.getPageNumber(), pageable.getPageSize(), query);
    }

    @Override
    public Immunization create(Immunization resource) {
        ImmunizationEntity entity = immunizationMapper.toEntity(resource);

        if (resource.hasPatient()) {
            String ref = resource.getPatient().getReference();
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

        if (resource.hasPerformer()) {
            String ref = resource.getPerformerFirstRep().getActor().getReference();
            if (ref != null && ref.contains("/")) {
                UUID practId = UUID.fromString(ref.split("/")[1]);
                entity.setPerformer(practitionerRepository.findById(practId)
                        .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found: " + practId)));
            }
        }

        ImmunizationEntity saved = immunizationRepository.save(entity);
        return immunizationMapper.toFhirResource(saved);
    }

    @Override
    public Immunization update(UUID id, Immunization resource) {
        ImmunizationEntity existing = immunizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Immunization not found: " + id));

        ImmunizationEntity updated = immunizationMapper.toEntity(resource);

        existing.setStatus(updated.getStatus());
        existing.setVaccineCodeSystem(updated.getVaccineCodeSystem());
        existing.setVaccineCodeValue(updated.getVaccineCodeValue());
        existing.setVaccineCodeDisplay(updated.getVaccineCodeDisplay());
        existing.setLotNumber(updated.getLotNumber());
        existing.setSiteCode(updated.getSiteCode());
        existing.setRouteCode(updated.getRouteCode());
        existing.setOccurrenceDate(updated.getOccurrenceDate());
        existing.setDoseQuantity(updated.getDoseQuantity());

        if (resource.hasPatient()) {
            String ref = resource.getPatient().getReference();
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

        if (resource.hasPerformer()) {
            String ref = resource.getPerformerFirstRep().getActor().getReference();
            if (ref != null && ref.contains("/")) {
                UUID practId = UUID.fromString(ref.split("/")[1]);
                existing.setPerformer(practitionerRepository.findById(practId)
                        .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found: " + practId)));
            }
        }

        ImmunizationEntity saved = immunizationRepository.save(existing);
        return immunizationMapper.toFhirResource(saved);
    }

    @Override
    public void delete(UUID id) {
        ImmunizationEntity entity = immunizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Immunization not found: " + id));
        immunizationRepository.delete(entity);
    }
}
