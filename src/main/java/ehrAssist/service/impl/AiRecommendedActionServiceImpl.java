package ehrAssist.service.impl;

import ehrAssist.dto.request.AiRecommendedActionRequest;
import ehrAssist.entity.AiRecommendedActionEntity;
import ehrAssist.exception.ResourceNotFoundException;
import ehrAssist.mapper.AiRecommendedActionMapper;
import ehrAssist.repository.AiRecommendedActionRepository;
import ehrAssist.repository.PatientRepository;
import ehrAssist.service.AiRecommendedActionService;
import ehrAssist.util.BundleBuilder;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AiRecommendedActionServiceImpl implements AiRecommendedActionService {

    private final AiRecommendedActionRepository aiRecommendedActionRepository;
    private final PatientRepository patientRepository;
    private final AiRecommendedActionMapper aiRecommendedActionMapper;
    private final BundleBuilder bundleBuilder;

    @Override
    public Communication create(AiRecommendedActionRequest request) {
        if (!patientRepository.existsById(request.getPatientId())) {
            throw new ResourceNotFoundException("Patient not found: " + request.getPatientId());
        }

        AiRecommendedActionEntity entity = AiRecommendedActionEntity.builder()
                .patientId(request.getPatientId())
                .practitionerId(request.getPractitionerId())
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .urgencyNote(ObjectUtils.isEmpty(request.getUrgencyNote()) ? null : request.getUrgencyNote())
                .verifiedAt(LocalDateTime.now())
                .verifiedBy(request.getPractitionerId())
                .build();

        AiRecommendedActionEntity saved = aiRecommendedActionRepository.save(entity);
        return aiRecommendedActionMapper.toFhirResource(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Bundle getByPatientId(UUID patientId, Pageable pageable) {
        Page<AiRecommendedActionEntity> data = aiRecommendedActionRepository.findByPatientId(patientId, pageable);
        List<AiRecommendedActionEntity> actions = data.getContent();

        if (!ObjectUtils.isEmpty(actions)) {
            List<Resource> fhirResources = actions.stream()
                    .map(aiRecommendedActionMapper::toFhirResource)
                    .map(Resource.class::cast)
                    .toList();

            StringBuilder queryParams = new StringBuilder();
            if (patientId != null) queryParams.append("patientId=").append(patientId).append("&");
            String query = queryParams.length() > 0 ? queryParams.substring(0, queryParams.length() - 1) : "";

            return bundleBuilder.searchSetWithPagination("Communication", fhirResources, data.getTotalElements(),
                    pageable.getPageNumber(), pageable.getPageSize(), query);
        }

        return bundleBuilder.searchSetWithPagination("Communication", null, data.getTotalElements(),
                pageable.getPageNumber(), pageable.getPageSize(), "");
    }
}
