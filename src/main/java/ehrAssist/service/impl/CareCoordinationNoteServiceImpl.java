package ehrAssist.service.impl;

import ehrAssist.dto.request.CreateCareCoordinationNoteRequest;
import ehrAssist.entity.AIRecommendedActionsEntity;
import ehrAssist.entity.CareCoordinationNoteEntity;
import ehrAssist.exception.FhirValidationException;
import ehrAssist.mapper.CareCoordinationNoteMapper;
import ehrAssist.repository.AIRecommendedActionsRepository;
import ehrAssist.repository.CareCoordinationNoteRepository;
import ehrAssist.service.CareCoordinationNoteService;
import ehrAssist.util.BundleBuilder;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CareCoordinationNoteServiceImpl implements CareCoordinationNoteService {

    private final CareCoordinationNoteRepository careCoordinationNoteRepository;
    private final CareCoordinationNoteMapper careCoordinationNoteMapper;
    private final BundleBuilder bundleBuilder;
    private final AIRecommendedActionsRepository aiRecommendedActionsRepository;

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
        if (request.getPatientId() == null) {
            throw new FhirValidationException("patientId is required");
        }
        if (request.getCoordinatorEmail() == null || request.getCoordinatorEmail().isBlank()) {
            throw new FhirValidationException("coordinatorEmail is required");
        }
        if (request.getCoordinatorName() == null || request.getCoordinatorName().isBlank()) {
            throw new FhirValidationException("coordinatorName is required");
        }
        if (request.getCoordinatorRole() == null || request.getCoordinatorRole().isBlank()) {
            throw new FhirValidationException("coordinatorRole is required");
        }
        if (request.getCareNotes() == null || request.getCareNotes().isBlank()) {
            throw new FhirValidationException("careNotes is required");
        }
        if (ObjectUtils.isEmpty(request.getActionId())) {
            throw new FhirValidationException("can't add a not without creating an action");
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
}
