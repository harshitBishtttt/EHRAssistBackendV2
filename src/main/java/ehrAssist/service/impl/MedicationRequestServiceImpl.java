package ehrAssist.service.impl;

import ehrAssist.entity.MedicationRequestEntity;
import ehrAssist.exception.ResourceNotFoundException;
import ehrAssist.mapper.MedicationRequestMapper;
import ehrAssist.repository.EncounterRepository;
import ehrAssist.repository.MedicationRequestRepository;
import ehrAssist.repository.PatientRepository;
import ehrAssist.repository.PractitionerRepository;
import ehrAssist.repository.master.MedicationCodeMasterRepository;
import ehrAssist.service.MedicationRequestService;
import ehrAssist.util.BundleBuilder;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.MedicationRequest;
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
public class MedicationRequestServiceImpl implements MedicationRequestService {

    private final MedicationRequestRepository medicationRequestRepository;
    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final PractitionerRepository practitionerRepository;
    private final MedicationCodeMasterRepository medicationCodeMasterRepository;
    private final MedicationRequestMapper medicationRequestMapper;
    private final BundleBuilder bundleBuilder;

    @Override
    @Transactional(readOnly = true)
    public MedicationRequest getById(UUID id) {
        MedicationRequestEntity entity = medicationRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicationRequest not found: " + id));
        return medicationRequestMapper.toFhirResource(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Bundle search(UUID id, UUID patientId, String status, Pageable pageable) {
        if (id == null && patientId == null && status == null) {
            return bundleBuilder.searchSetWithPagination("MedicationRequest", List.of(), 0L, 0, pageable.getPageSize(), "");
        }

        Specification<MedicationRequestEntity> spec = Specification.where(null);

        if (id != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("id"), id));
        }
        if (patientId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("patient").get("id"), patientId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        Page<MedicationRequestEntity> pageResult = medicationRequestRepository.findAll(spec, pageable);

        List<Resource> fhirResources = pageResult.getContent().stream()
                .map(medicationRequestMapper::toFhirResource)
                .map(Resource.class::cast)
                .toList();

        StringBuilder queryParams = new StringBuilder();
        if (id != null) queryParams.append("_id=").append(id).append("&");
        if (patientId != null) queryParams.append("patient=").append(patientId).append("&");
        if (status != null) queryParams.append("status=").append(status).append("&");
        String query = queryParams.length() > 0 ? queryParams.substring(0, queryParams.length() - 1) : "";

        return bundleBuilder.searchSetWithPagination("MedicationRequest", fhirResources, pageResult.getTotalElements(), 
                pageable.getPageNumber(), pageable.getPageSize(), query);
    }

    @Override
    public MedicationRequest create(MedicationRequest resource) {
        MedicationRequestEntity entity = medicationRequestMapper.toEntity(resource);

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

        if (resource.hasMedicationCodeableConcept()) {
            CodeableConcept med = resource.getMedicationCodeableConcept();
            if (med.hasCoding()) {
                String codeValue = med.getCodingFirstRep().getCode();
                medicationCodeMasterRepository.findByCodeValue(codeValue)
                        .ifPresent(entity::setMedicationCode);
            }
        }

        MedicationRequestEntity saved = medicationRequestRepository.save(entity);
        return medicationRequestMapper.toFhirResource(saved);
    }

    @Override
    public MedicationRequest update(UUID id, MedicationRequest resource) {
        MedicationRequestEntity existing = medicationRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicationRequest not found: " + id));

        MedicationRequestEntity updated = medicationRequestMapper.toEntity(resource);

        existing.setStatus(updated.getStatus());
        existing.setIntent(updated.getIntent());
        existing.setPriority(updated.getPriority());
        existing.setDosageText(updated.getDosageText());
        existing.setDosageRouteCode(updated.getDosageRouteCode());
        existing.setDosageRouteDisplay(updated.getDosageRouteDisplay());
        existing.setDoseUnit(updated.getDoseUnit());
        existing.setFrequencyText(updated.getFrequencyText());
        existing.setReasonCode(updated.getReasonCode());
        existing.setReasonDisplay(updated.getReasonDisplay());
        existing.setDoseValue(updated.getDoseValue());
        existing.setNote(updated.getNote());
        existing.setAuthoredOn(updated.getAuthoredOn());
        existing.setValidStart(updated.getValidStart());
        existing.setValidEnd(updated.getValidEnd());

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

        if (resource.hasMedicationCodeableConcept()) {
            CodeableConcept med = resource.getMedicationCodeableConcept();
            if (med.hasCoding()) {
                String codeValue = med.getCodingFirstRep().getCode();
                medicationCodeMasterRepository.findByCodeValue(codeValue)
                        .ifPresent(existing::setMedicationCode);
            }
        }

        MedicationRequestEntity saved = medicationRequestRepository.save(existing);
        return medicationRequestMapper.toFhirResource(saved);
    }

    @Override
    public void delete(UUID id) {
        MedicationRequestEntity entity = medicationRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicationRequest not found: " + id));
        medicationRequestRepository.delete(entity);
    }
}
