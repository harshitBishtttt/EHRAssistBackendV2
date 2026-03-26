package ehrAssist.service.impl;

import ehrAssist.entity.DiagnosticReportEntity;
import ehrAssist.exception.ResourceNotFoundException;
import ehrAssist.mapper.DiagnosticReportMapper;
import ehrAssist.repository.DiagnosticReportRepository;
import ehrAssist.repository.EncounterRepository;
import ehrAssist.repository.PatientRepository;
import ehrAssist.repository.PractitionerRepository;
import ehrAssist.service.DiagnosticReportService;
import ehrAssist.util.BundleBuilder;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
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
public class DiagnosticReportServiceImpl implements DiagnosticReportService {

    private final DiagnosticReportRepository diagnosticReportRepository;
    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final PractitionerRepository practitionerRepository;
    private final DiagnosticReportMapper diagnosticReportMapper;
    private final BundleBuilder bundleBuilder;

    @Override
    @Transactional(readOnly = true)
    public DiagnosticReport getById(UUID id) {
        DiagnosticReportEntity entity = diagnosticReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DiagnosticReport not found: " + id));
        return diagnosticReportMapper.toFhirResource(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Bundle search(UUID id, UUID patientId, Pageable pageable) {
        // If no parameters provided, return empty bundle
        if (id == null && patientId == null) {
            return bundleBuilder.searchSetWithPagination("DiagnosticReport", List.of(), 0L, 0, pageable.getPageSize(), "");
        }

        // Build specification with all provided parameters (AND logic)
        Specification<DiagnosticReportEntity> spec = Specification.where(null);
        
        if (id != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("id"), id));
        }
        if (patientId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("patient").get("id"), patientId));
        }

        Page<DiagnosticReportEntity> pageResult = diagnosticReportRepository.findAll(spec, pageable);

        List<Resource> fhirResources = pageResult.getContent().stream()
                .map(diagnosticReportMapper::toFhirResource)
                .map(Resource.class::cast)
                .toList();

        StringBuilder queryParams = new StringBuilder();
        if (id != null) queryParams.append("_id=").append(id).append("&");
        if (patientId != null) queryParams.append("patient=").append(patientId).append("&");
        String query = queryParams.length() > 0 ? queryParams.substring(0, queryParams.length() - 1) : "";

        return bundleBuilder.searchSetWithPagination("DiagnosticReport", fhirResources, pageResult.getTotalElements(),
                pageable.getPageNumber(), pageable.getPageSize(), query);
    }

    @Override
    public DiagnosticReport create(DiagnosticReport resource) {
        DiagnosticReportEntity entity = diagnosticReportMapper.toEntity(resource);

        if (resource.hasSubject()) {
            String ref = resource.getSubject().getReference();
            if (ref != null && ref.contains("/")) {
                UUID patientId = UUID.fromString(ref.split("/")[1]);
                entity.setPatient(patientRepository.findById(patientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId)));
            }
        }

        if (resource.hasEncounter()) {
            String ref = resource.getEncounter().getReference();
            if (ref != null && ref.contains("/")) {
                UUID encId = UUID.fromString(ref.split("/")[1]);
                entity.setEncounter(encounterRepository.findById(encId)
                        .orElseThrow(() -> new ResourceNotFoundException("Encounter not found: " + encId)));
            }
        }

        if (resource.hasPerformer()) {
            String ref = resource.getPerformerFirstRep().getReference();
            if (ref != null && ref.contains("/")) {
                UUID practId = UUID.fromString(ref.split("/")[1]);
                entity.setPerformer(practitionerRepository.findById(practId)
                        .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found: " + practId)));
            }
        }

        DiagnosticReportEntity saved = diagnosticReportRepository.save(entity);
        return diagnosticReportMapper.toFhirResource(saved);
    }

    @Override
    public DiagnosticReport update(UUID id, DiagnosticReport resource) {
        DiagnosticReportEntity existing = diagnosticReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DiagnosticReport not found: " + id));

        DiagnosticReportEntity updated = diagnosticReportMapper.toEntity(resource);

        existing.setStatus(updated.getStatus());
        existing.setCategoryCode(updated.getCategoryCode());
        existing.setCodeSystem(updated.getCodeSystem());
        existing.setCodeValue(updated.getCodeValue());
        existing.setCodeDisplay(updated.getCodeDisplay());
        existing.setEffectiveDate(updated.getEffectiveDate());
        existing.setIssued(updated.getIssued());
        existing.setConclusion(updated.getConclusion());

        if (resource.hasSubject()) {
            String ref = resource.getSubject().getReference();
            if (ref != null && ref.contains("/")) {
                UUID patientId = UUID.fromString(ref.split("/")[1]);
                existing.setPatient(patientRepository.findById(patientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId)));
            }
        }

        if (resource.hasEncounter()) {
            String ref = resource.getEncounter().getReference();
            if (ref != null && ref.contains("/")) {
                UUID encId = UUID.fromString(ref.split("/")[1]);
                existing.setEncounter(encounterRepository.findById(encId)
                        .orElseThrow(() -> new ResourceNotFoundException("Encounter not found: " + encId)));
            }
        }

        if (resource.hasPerformer()) {
            String ref = resource.getPerformerFirstRep().getReference();
            if (ref != null && ref.contains("/")) {
                UUID practId = UUID.fromString(ref.split("/")[1]);
                existing.setPerformer(practitionerRepository.findById(practId)
                        .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found: " + practId)));
            }
        }

        DiagnosticReportEntity saved = diagnosticReportRepository.save(existing);
        return diagnosticReportMapper.toFhirResource(saved);
    }

    @Override
    public void delete(UUID id) {
        DiagnosticReportEntity entity = diagnosticReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DiagnosticReport not found: " + id));
        diagnosticReportRepository.delete(entity);
    }
}
