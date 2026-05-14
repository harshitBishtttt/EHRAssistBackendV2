package ehrAssist.service.impl;

import ehrAssist.dto.request.CreateCareCoordinationNoteRequest;
import ehrAssist.entity.AIRecommendedActionsEntity;
import ehrAssist.entity.CareCoordinationNoteEntity;
import ehrAssist.entity.PatientEntity;
import ehrAssist.exception.FhirValidationException;
import ehrAssist.mapper.CareCoordinationNoteMapper;
import ehrAssist.mapper.PatientMapper;
import ehrAssist.repository.AIRecommendedActionsRepository;
import ehrAssist.repository.CareCoordinationNoteRepository;
import ehrAssist.repository.PatientRepository;
import ehrAssist.service.CareManagerService;
import ehrAssist.util.BundleBuilder;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
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
public class CareManagerServiceImpl implements CareManagerService {

    private final CareCoordinationNoteRepository careCoordinationNoteRepository;
    private final CareCoordinationNoteMapper careCoordinationNoteMapper;
    private final BundleBuilder bundleBuilder;
    private final AIRecommendedActionsRepository aiRecommendedActionsRepository;
    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

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
    @Transactional(readOnly = true)
    public Bundle fetchPatientsByCareManager(UUID careManagerId, Pageable pageable) {
        Page<PatientEntity> data = patientRepository.findAllByPrimaryCareManagerId(careManagerId, pageable);
        List<PatientEntity> patientData = data.getContent();
        if (!ObjectUtils.isEmpty(patientData)) {
            List<Resource> fhirResources = patientData.stream()
                    .map(patientMapper::toFhirResource)
                    .map(Resource.class::cast)
                    .toList();
            StringBuilder queryParams = new StringBuilder();
            if (careManagerId != null) queryParams.append("id=").append(careManagerId).append("&");
            String query = queryParams.length() > 0 ? queryParams.substring(0, queryParams.length() - 1) : "";
            return bundleBuilder.searchSetWithPagination("Patient", fhirResources, data.getTotalElements(),
                    pageable.getPageNumber(), pageable.getPageSize(), query);
        }
        return bundleBuilder.searchSetWithPagination("Patient", List.of(), data.getTotalElements(),
                pageable.getPageNumber(), pageable.getPageSize(), "");
    }
}
