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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
    public Bundle getVerifiedByPatientId(UUID patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found: " + patientId);
        }

        List<Resource> resources = aiRecommendationRepository
                .findByPatientIdAndVerifiedAtIsNotNull(patientId)
                .stream()
                .map(aiRecommendationMapper::toFhirResource)
                .map(Resource.class::cast)
                .collect(Collectors.toList());

        return bundleBuilder.searchSet("Communication", resources, resources.size());
    }
}
