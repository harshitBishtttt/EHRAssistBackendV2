package ehrAssist.service.impl;

import ehrAssist.dto.request.CreateCareCoordinationNoteRequest;
import ehrAssist.dto.request.CreateP360RiskScoreRequest;
import ehrAssist.entity.AIRecommendedActionsEntity;
import ehrAssist.entity.CareCoordinationNoteEntity;
import ehrAssist.entity.P360RiskScoreEntity;
import ehrAssist.entity.PatientEntity;
import ehrAssist.exception.FhirValidationException;
import ehrAssist.exception.ResourceNotFoundException;
import ehrAssist.mapper.CareCoordinationNoteMapper;
import ehrAssist.mapper.P360RiskScoreMapper;
import ehrAssist.mapper.PatientMapper;
import ehrAssist.repository.AIRecommendedActionsRepository;
import ehrAssist.repository.CareCoordinationNoteRepository;
import ehrAssist.repository.OrganizationRepository;
import ehrAssist.repository.P360RiskScoreRepository;
import ehrAssist.repository.PatientRepository;
import ehrAssist.repository.PractitionerRepository;
import ehrAssist.service.CareManagerService;
import ehrAssist.util.BundleBuilder;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.RiskAssessment;
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
public class CareManagerServiceImpl implements CareManagerService {

    private final CareCoordinationNoteRepository careCoordinationNoteRepository;
    private final CareCoordinationNoteMapper careCoordinationNoteMapper;
    private final BundleBuilder bundleBuilder;
    private final AIRecommendedActionsRepository aiRecommendedActionsRepository;
    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final P360RiskScoreRepository riskScoreRepository;
    private final P360RiskScoreMapper riskScoreMapper;
    private final PractitionerRepository practitionerRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional(readOnly = true)
    public Bundle search(UUID patientId, String coordinatorEmail, UUID actionId, String status) {
        List<Resource> resources = careCoordinationNoteRepository
                .findByIsActiveAndPatientIdAndCoordinatorEmailAndAiRecommendedActionsEntity_IdAndStatus
                        (true, patientId, coordinatorEmail, actionId, status)
                .stream()
                .map(careCoordinationNoteMapper::toFhirResource)
                .map(Resource.class::cast)
                .toList();

        return bundleBuilder.searchSet("DocumentReference", resources, resources.size());
    }

    @Override
    @Transactional
    public DocumentReference create(CreateCareCoordinationNoteRequest request) {
        if (ObjectUtils.isEmpty(request.getPatientId())) {
            throw new FhirValidationException("Patient Id missing");
        }
        if (ObjectUtils.isEmpty(request.getStatus())) {
            throw new FhirValidationException("Status is missing");
        }
        if (ObjectUtils.isEmpty(request.getActionId())) {
            throw new FhirValidationException("Action Id missing");
        }
        if (ObjectUtils.isEmpty(request.getCoordinatorEmail())) {
            throw new FhirValidationException("Coordinator Email missing");
        }
        AIRecommendedActionsEntity againstThisAction = aiRecommendedActionsRepository
                .findById(request.getActionId()).orElseThrow(() -> new FhirValidationException(request.getActionId() + "Particular Action ID is not present"));
        CareCoordinationNoteEntity entity = CareCoordinationNoteEntity.builder()
                .patientId(request.getPatientId())
                .coordinatorEmail(request.getCoordinatorEmail())
                .coordinatorName(request.getCoordinatorName())
                .coordinatorRole(request.getCoordinatorRole())
                .careNotes(request.getCareNotes())
                .isActive(true)
                .aiRecommendedActionsEntity(againstThisAction)
                .status(request.getStatus())
                .createdAt(LocalDateTime.now())
                .build();

        CareCoordinationNoteEntity saved = careCoordinationNoteRepository.save(entity);
        return careCoordinationNoteMapper.toFhirResource(saved);
    }

    public void deactivateNotes(String email, UUID patientId, UUID actionId, String status) {
        careCoordinationNoteRepository.deactivateTheActivity(email, patientId, actionId, status);
    }


    @Override
    @Transactional
    public RiskAssessment createRiskScore(CreateP360RiskScoreRequest request) {
        if (ObjectUtils.isEmpty(request.getCareManagerId())) {
            throw new FhirValidationException("careManagerId is required for care manager risk score");
        }

        P360RiskScoreEntity entity = P360RiskScoreEntity.builder()
                .riskScore(request.getRiskScore())
                .createdDate(LocalDateTime.now())
                .patient(patientRepository.findById(request.getPatientId())
                        .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + request.getPatientId())))
                .careManager(practitionerRepository.findById(request.getCareManagerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Care Manager not found: " + request.getCareManagerId())))
                .organization(organizationRepository.findById(request.getOrganizationId())
                        .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + request.getOrganizationId())))
                .build();

        P360RiskScoreEntity saved = riskScoreRepository.save(entity);
        return riskScoreMapper.toFhirResource(saved);
    }
}
