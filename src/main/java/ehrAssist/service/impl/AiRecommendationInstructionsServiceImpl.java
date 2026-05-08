package ehrAssist.service.impl;

import ehrAssist.dto.request.AiRecommendationInstructionsRequest;
import ehrAssist.entity.AiRecommendationInstructionsEntity;
import ehrAssist.entity.AiRecommendationInstructionsPayloadEntity;
import ehrAssist.exception.ResourceNotFoundException;
import ehrAssist.mapper.AiRecommendationInstructionsMapper;
import ehrAssist.repository.AiRecommendationInstructionsRepository;
import ehrAssist.repository.PatientRepository;
import ehrAssist.service.AiRecommendationInstructionsService;
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
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Transactional
public class AiRecommendationInstructionsServiceImpl implements AiRecommendationInstructionsService {

    private final AiRecommendationInstructionsRepository aiRecommendationInstructionsRepository;
    private final PatientRepository patientRepository;
    private final AiRecommendationInstructionsMapper aiRecommendationInstructionsMapper;
    private final BundleBuilder bundleBuilder;

    @Override
    public Communication create(AiRecommendationInstructionsRequest request) {
        if (!patientRepository.existsById(request.getPatientId())) {
            throw new ResourceNotFoundException("Patient not found: " + request.getPatientId());
        }

        AiRecommendationInstructionsEntity entity = AiRecommendationInstructionsEntity.builder()
                .patientId(request.getPatientId())
                .practitionerId(request.getPractitionerId())
                .verifiedAt(LocalDateTime.now())
                .build();

        AtomicInteger seq = new AtomicInteger(1);
        List<AiRecommendationInstructionsPayloadEntity> payloads = request.getPayloads().stream()
                .map(text -> AiRecommendationInstructionsPayloadEntity.builder()
                        .recommendation(entity)
                        .sequence(seq.getAndIncrement())
                        .contentString(text)
                        .build())
                .toList();
        entity.getPayloads().addAll(payloads);

        AiRecommendationInstructionsEntity saved = aiRecommendationInstructionsRepository.save(entity);
        return aiRecommendationInstructionsMapper.toFhirResource(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Bundle getByPatientId(UUID patientId, Pageable pageable) {
        Page<AiRecommendationInstructionsEntity> data = aiRecommendationInstructionsRepository.findByPatientId(patientId, pageable);
        List<AiRecommendationInstructionsEntity> recommendations = data.getContent();

        if (!ObjectUtils.isEmpty(recommendations)) {
            List<Resource> fhirResources = recommendations.stream()
                    .map(aiRecommendationInstructionsMapper::toFhirResource)
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
