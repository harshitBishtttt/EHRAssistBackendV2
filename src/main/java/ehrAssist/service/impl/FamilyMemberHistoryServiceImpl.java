package ehrAssist.service.impl;

import ehrAssist.entity.FamilyMemberHistoryConditionEntity;
import ehrAssist.entity.FamilyMemberHistoryEntity;
import ehrAssist.exception.ResourceNotFoundException;
import ehrAssist.mapper.FamilyMemberHistoryMapper;
import ehrAssist.repository.FamilyMemberHistoryRepository;
import ehrAssist.repository.PatientRepository;
import ehrAssist.service.FamilyMemberHistoryService;
import ehrAssist.util.BundleBuilder;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.FamilyMemberHistory;
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
public class FamilyMemberHistoryServiceImpl implements FamilyMemberHistoryService {

    private final FamilyMemberHistoryRepository familyMemberHistoryRepository;
    private final PatientRepository patientRepository;
    private final FamilyMemberHistoryMapper familyMemberHistoryMapper;
    private final BundleBuilder bundleBuilder;

    @Override
    @Transactional(readOnly = true)
    public FamilyMemberHistory getById(UUID id) {
        FamilyMemberHistoryEntity entity = familyMemberHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FamilyMemberHistory not found: " + id));
        return familyMemberHistoryMapper.toFhirResource(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Bundle search(UUID id, UUID patientId, String relationship, Pageable pageable) {
        if (id == null && patientId == null && relationship == null) {
            return bundleBuilder.searchSetWithPagination("FamilyMemberHistory", List.of(), 0L, 0, pageable.getPageSize(), "");
        }

        Specification<FamilyMemberHistoryEntity> spec = Specification.where(null);

        if (id != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("id"), id));
        }
        if (patientId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("patient").get("id"), patientId));
        }
        if (relationship != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("relationshipCode"), relationship));
        }

        Page<FamilyMemberHistoryEntity> pageResult = familyMemberHistoryRepository.findAll(spec, pageable);

        List<Resource> fhirResources = pageResult.getContent().stream()
                .map(familyMemberHistoryMapper::toFhirResource)
                .map(Resource.class::cast)
                .toList();

        StringBuilder queryParams = new StringBuilder();
        if (id != null) queryParams.append("_id=").append(id).append("&");
        if (patientId != null) queryParams.append("patient=").append(patientId).append("&");
        if (relationship != null) queryParams.append("relationship=").append(relationship).append("&");
        String query = queryParams.length() > 0 ? queryParams.substring(0, queryParams.length() - 1) : "";

        return bundleBuilder.searchSetWithPagination("FamilyMemberHistory", fhirResources, pageResult.getTotalElements(),
                pageable.getPageNumber(), pageable.getPageSize(), query);
    }

    @Override
    public FamilyMemberHistory create(FamilyMemberHistory resource) {
        FamilyMemberHistoryEntity entity = familyMemberHistoryMapper.toEntity(resource);

        if (resource.hasPatient()) {
            String ref = resource.getPatient().getReference();
            if (ref != null && ref.contains("/")) {
                UUID patientId = UUID.fromString(ref.split("/")[1]);
                entity.setPatient(patientRepository.findById(patientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId)));
            }
        }

        FamilyMemberHistoryEntity saved = familyMemberHistoryRepository.save(entity);
        return familyMemberHistoryMapper.toFhirResource(saved);
    }

    @Override
    public FamilyMemberHistory update(UUID id, FamilyMemberHistory resource) {
        FamilyMemberHistoryEntity existing = familyMemberHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FamilyMemberHistory not found: " + id));

        FamilyMemberHistoryEntity updated = familyMemberHistoryMapper.toEntity(resource);

        existing.setStatus(updated.getStatus());
        existing.setName(updated.getName());
        existing.setRelationshipSystem(updated.getRelationshipSystem());
        existing.setRelationshipCode(updated.getRelationshipCode());
        existing.setRelationshipDisplay(updated.getRelationshipDisplay());
        existing.setSex(updated.getSex());
        existing.setBornDate(updated.getBornDate());
        existing.setAgeValue(updated.getAgeValue());
        existing.setAgeUnit(updated.getAgeUnit());
        existing.setEstimatedAge(updated.getEstimatedAge());
        existing.setDeceasedFlag(updated.getDeceasedFlag());
        existing.setDeceasedAgeValue(updated.getDeceasedAgeValue());
        existing.setDeceasedAgeUnit(updated.getDeceasedAgeUnit());
        existing.setDeceasedDate(updated.getDeceasedDate());
        existing.setNote(updated.getNote());

        existing.getConditions().clear();
        for (FamilyMemberHistoryConditionEntity condEntity : updated.getConditions()) {
            condEntity.setFamilyMemberHistory(existing);
            existing.getConditions().add(condEntity);
        }

        if (resource.hasPatient()) {
            String ref = resource.getPatient().getReference();
            if (ref != null && ref.contains("/")) {
                UUID patientId = UUID.fromString(ref.split("/")[1]);
                existing.setPatient(patientRepository.findById(patientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId)));
            }
        }

        FamilyMemberHistoryEntity saved = familyMemberHistoryRepository.save(existing);
        return familyMemberHistoryMapper.toFhirResource(saved);
    }

    @Override
    public void delete(UUID id) {
        FamilyMemberHistoryEntity entity = familyMemberHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FamilyMemberHistory not found: " + id));
        familyMemberHistoryRepository.delete(entity);
    }
}
