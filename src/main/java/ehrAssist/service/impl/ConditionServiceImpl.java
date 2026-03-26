package ehrAssist.service.impl;

import ehrAssist.entity.ConditionEntity;
import ehrAssist.exception.ResourceNotFoundException;
import ehrAssist.mapper.ConditionMapper;
import ehrAssist.repository.ConditionRepository;
import ehrAssist.repository.EncounterRepository;
import ehrAssist.repository.PatientRepository;
import ehrAssist.repository.PractitionerRepository;
import ehrAssist.repository.master.ConditionCodeMasterRepository;
import ehrAssist.service.ConditionService;
import ehrAssist.util.BundleBuilder;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
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
public class ConditionServiceImpl implements ConditionService {

    private final ConditionRepository conditionRepository;
    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final PractitionerRepository practitionerRepository;
    private final ConditionCodeMasterRepository conditionCodeMasterRepository;
    private final ConditionMapper conditionMapper;
    private final BundleBuilder bundleBuilder;

    @Override
    @Transactional(readOnly = true)
    public Condition getById(UUID id) {
        ConditionEntity entity = conditionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Condition not found: " + id));
        return conditionMapper.toFhirResource(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Bundle search(UUID id, UUID patientId, String code, Pageable pageable) {
        if (id == null && patientId == null && code == null) {
            return bundleBuilder.searchSetWithPagination("Condition", List.of(), 0L, 0, pageable.getPageSize(), "");
        }

        Specification<ConditionEntity> spec = Specification.where(null);

        if (id != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("id"), id));
        }
        if (patientId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("patient").get("id"), patientId));
        }
        if (code != null) {
            spec = spec.and((root, query, cb) -> {
                var codeJoin = root.join("codeMaster");
                return cb.equal(codeJoin.get("codeValue"), code);
            });
        }

        Page<ConditionEntity> pageResult = conditionRepository.findAll(spec, pageable);
        
        List<Resource> fhirResources = pageResult.getContent().stream()
                .map(conditionMapper::toFhirResource)
                .map(Resource.class::cast)
                .toList();

        StringBuilder queryParams = new StringBuilder();
        if (id != null) queryParams.append("_id=").append(id).append("&");
        if (patientId != null) queryParams.append("patient=").append(patientId).append("&");
        if (code != null) queryParams.append("code=").append(code).append("&");
        String query = queryParams.length() > 0 ? queryParams.substring(0, queryParams.length() - 1) : "";

        return bundleBuilder.searchSetWithPagination("Condition", fhirResources, pageResult.getTotalElements(), 
                pageable.getPageNumber(), pageable.getPageSize(), query);
    }

    @Override
    public Condition create(Condition resource) {
        ConditionEntity entity = conditionMapper.toEntity(resource);

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

        if (resource.hasRecorder()) {
            String ref = resource.getRecorder().getReference();
            if (ref != null && ref.contains("/")) {
                UUID practId = UUID.fromString(ref.split("/")[1]);
                entity.setRecorder(practitionerRepository.findById(practId)
                        .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found: " + practId)));
            }
        }

        if (resource.hasCode() && resource.getCode().hasCoding()) {
            Coding coding = resource.getCode().getCodingFirstRep();
            conditionCodeMasterRepository.findByCodeSystemAndCodeValue(coding.getSystem(), coding.getCode())
                    .ifPresent(entity::setCodeMaster);
        }

        ConditionEntity saved = conditionRepository.save(entity);
        return conditionMapper.toFhirResource(saved);
    }

    @Override
    public Condition update(UUID id, Condition resource) {
        ConditionEntity existing = conditionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Condition not found: " + id));

        ConditionEntity updated = conditionMapper.toEntity(resource);

        existing.setClinicalStatus(updated.getClinicalStatus());
        existing.setVerificationStatus(updated.getVerificationStatus());
        existing.setSeverityCode(updated.getSeverityCode());
        existing.setSeverityDisplay(updated.getSeverityDisplay());
        existing.setOnsetDate(updated.getOnsetDate());
        existing.setAbatementDate(updated.getAbatementDate());
        existing.setRecordedDate(updated.getRecordedDate());

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

        if (resource.hasRecorder()) {
            String ref = resource.getRecorder().getReference();
            if (ref != null && ref.contains("/")) {
                UUID practId = UUID.fromString(ref.split("/")[1]);
                existing.setRecorder(practitionerRepository.findById(practId)
                        .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found: " + practId)));
            }
        }

        if (resource.hasCode() && resource.getCode().hasCoding()) {
            Coding coding = resource.getCode().getCodingFirstRep();
            conditionCodeMasterRepository.findByCodeSystemAndCodeValue(coding.getSystem(), coding.getCode())
                    .ifPresent(existing::setCodeMaster);
        }

        ConditionEntity saved = conditionRepository.save(existing);
        return conditionMapper.toFhirResource(saved);
    }

    @Override
    public void delete(UUID id) {
        ConditionEntity entity = conditionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Condition not found: " + id));
        conditionRepository.delete(entity);
    }
}
