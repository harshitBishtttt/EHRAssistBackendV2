package ehrAssist.service.impl;

import ehrAssist.dto.request.CreateCareCoordinationNoteRequest;
import ehrAssist.entity.CareCoordinationNoteEntity;
import ehrAssist.exception.FhirValidationException;
import ehrAssist.mapper.CareCoordinationNoteMapper;
import ehrAssist.repository.CareCoordinationNoteRepository;
import ehrAssist.service.CareCoordinationNoteService;
import ehrAssist.util.BundleBuilder;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CareCoordinationNoteServiceImpl implements CareCoordinationNoteService {

    private final CareCoordinationNoteRepository careCoordinationNoteRepository;
    private final CareCoordinationNoteMapper careCoordinationNoteMapper;
    private final BundleBuilder bundleBuilder;

    @Override
    @Transactional(readOnly = true)
    public Bundle search(UUID patientId, String coordinatorEmail) {
        List<Resource> resources = careCoordinationNoteRepository
                .findByPatientIdAndCoordinatorEmail(patientId, coordinatorEmail)
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
        if (request.getCareNotes() == null || request.getCareNotes().isBlank()) {
            throw new FhirValidationException("careNotes is required");
        }
        if (request.getCreatedAt() == null) {
            throw new FhirValidationException("createdAt is required");
        }

        CareCoordinationNoteEntity entity = CareCoordinationNoteEntity.builder()
                .patientId(request.getPatientId())
                .coordinatorEmail(request.getCoordinatorEmail())
                .careNotes(request.getCareNotes())
                .createdAt(request.getCreatedAt())
                .build();

        CareCoordinationNoteEntity saved = careCoordinationNoteRepository.save(entity);
        return careCoordinationNoteMapper.toFhirResource(saved);
    }
}
