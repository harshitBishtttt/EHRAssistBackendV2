package ehrAssist.service.impl;

import ehrAssist.dto.request.AiRecommendationRequest;
import ehrAssist.entity.AiRecommendationEntity;
import ehrAssist.entity.AiRecommendationPayloadEntity;
import ehrAssist.exception.ResourceNotFoundException;
import ehrAssist.mapper.AiRecommendationMapper;
import ehrAssist.repository.AiRecommendationRepository;
import ehrAssist.repository.PatientRepository;
import ehrAssist.service.AiRecommendationService;
import lombok.RequiredArgsConstructor;
import ehrAssist.util.BundleBuilder;
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
public class AiRecommendationServiceImpl implements AiRecommendationService {

    private final AiRecommendationRepository aiRecommendationRepository;
    private final PatientRepository patientRepository;
    private final AiRecommendationMapper aiRecommendationMapper;
    private final BundleBuilder bundleBuilder;

    @Override
    public Communication create(AiRecommendationRequest request) {
        if (!patientRepository.existsById(request.getPatientId())) {
            throw new ResourceNotFoundException("Patient not found: " + request.getPatientId());
        }

        AiRecommendationEntity entity = AiRecommendationEntity.builder()
                .patientId(request.getPatientId())
                .practitionerId(request.getPractitionerId())
                .verifiedAt(LocalDateTime.now())
                .build();

        AtomicInteger seq = new AtomicInteger(1);
        List<AiRecommendationPayloadEntity> payloads = request.getPayloads().stream()
                .map(text -> AiRecommendationPayloadEntity.builder()
                        .recommendation(entity)
                        .sequence(seq.getAndIncrement())
                        .contentString(text)

                        .build())
                .toList();
        entity.getPayloads().addAll(payloads);

        AiRecommendationEntity saved = aiRecommendationRepository.save(entity);
        return aiRecommendationMapper.toFhirResource(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Bundle getByPatientId(UUID patientId, Pageable pageable) {
        Page<AiRecommendationEntity> data = aiRecommendationRepository.findByPatientId(patientId, pageable);
        List<AiRecommendationEntity> recommendations = data.getContent();

        if (!ObjectUtils.isEmpty(recommendations)) {
            List<Resource> fhirResources = recommendations.stream()
                    .map(aiRecommendationMapper::toFhirResource)
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
