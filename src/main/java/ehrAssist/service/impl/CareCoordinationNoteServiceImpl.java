package ehrAssist.service.impl;

import ehrAssist.mapper.CareCoordinationNoteMapper;
import ehrAssist.repository.CareCoordinationNoteRepository;
import ehrAssist.service.CareCoordinationNoteService;
import ehrAssist.util.BundleBuilder;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareCoordinationNoteServiceImpl implements CareCoordinationNoteService {

    private final CareCoordinationNoteRepository careCoordinationNoteRepository;
    private final CareCoordinationNoteMapper careCoordinationNoteMapper;
    private final BundleBuilder bundleBuilder;

    @Override
    public Bundle search(UUID patientId, String coordinatorEmail) {
        List<Resource> resources = careCoordinationNoteRepository
                .findByPatientIdAndCoordinatorEmail(patientId, coordinatorEmail)
                .stream()
                .map(careCoordinationNoteMapper::toFhirResource)
                .map(Resource.class::cast)
                .toList();

        return bundleBuilder.searchSet("DocumentReference", resources, resources.size());
    }
}
