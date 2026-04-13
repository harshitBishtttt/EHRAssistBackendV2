package ehrAssist.service.impl;

import ehrAssist.entity.DocumentReferenceEntity;
import ehrAssist.exception.ResourceNotFoundException;
import ehrAssist.mapper.DocumentReferenceMapper;
import ehrAssist.repository.DocumentReferenceRepository;
import ehrAssist.repository.EncounterRepository;
import ehrAssist.repository.PatientRepository;
import ehrAssist.repository.PractitionerRepository;
import ehrAssist.service.DocumentReferenceService;
import ehrAssist.util.BundleBuilder;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
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
public class DocumentReferenceServiceImpl implements DocumentReferenceService {

    private final DocumentReferenceRepository documentReferenceRepository;
    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final PractitionerRepository practitionerRepository;
    private final DocumentReferenceMapper documentReferenceMapper;
    private final BundleBuilder bundleBuilder;

    @Override
    @Transactional(readOnly = true)
    public DocumentReference getById(UUID id) {
        DocumentReferenceEntity entity = documentReferenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentReference not found: " + id));
        return documentReferenceMapper.toFhirResource(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Bundle search(UUID id, UUID patientId, String typeCoding, Pageable pageable) {
        if (id == null && patientId == null && (typeCoding == null || typeCoding.isBlank())) {
            return bundleBuilder.searchSetWithPagination("DocumentReference", List.of(), 0L, 0, pageable.getPageSize(), "");
        }

        Specification<DocumentReferenceEntity> spec = Specification.where(null);
        
        if (id != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("id"), id));
        }
        if (patientId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("patient").get("id"), patientId));
        }
        if (typeCoding != null && !typeCoding.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("typeCode"), typeCoding));
        }

        Page<DocumentReferenceEntity> pageResult = documentReferenceRepository.findAll(spec, pageable);

        List<Resource> fhirResources = pageResult.getContent().stream()
                .map(documentReferenceMapper::toFhirResource)
                .map(Resource.class::cast)
                .toList();

        StringBuilder queryParams = new StringBuilder();
        if (id != null) queryParams.append("_id=").append(id).append("&");
        if (patientId != null) queryParams.append("patient=").append(patientId).append("&");
        if (typeCoding != null && !typeCoding.isBlank()) queryParams.append("type.coding=").append(typeCoding).append("&");
        String query = queryParams.length() > 0 ? queryParams.substring(0, queryParams.length() - 1) : "";

        return bundleBuilder.searchSetWithPagination("DocumentReference", fhirResources, pageResult.getTotalElements(), 
                pageable.getPageNumber(), pageable.getPageSize(), query);
    }

    @Override
    public DocumentReference create(DocumentReference resource) {
        DocumentReferenceEntity entity = documentReferenceMapper.toEntity(resource);

        if (resource.hasSubject()) {
            String ref = resource.getSubject().getReference();
            if (ref != null && ref.contains("/")) {
                UUID patientId = UUID.fromString(ref.split("/")[1]);
                entity.setPatient(patientRepository.findById(patientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId)));
            }
        }

        if (resource.hasContext() && resource.getContext().hasEncounter()) {
            String ref = resource.getContext().getEncounterFirstRep().getReference();
            if (ref != null && ref.contains("/")) {
                UUID encId = UUID.fromString(ref.split("/")[1]);
                entity.setEncounter(encounterRepository.findById(encId)
                        .orElseThrow(() -> new ResourceNotFoundException("Encounter not found: " + encId)));
            }
        }

        if (resource.hasAuthor()) {
            String ref = resource.getAuthorFirstRep().getReference();
            if (ref != null && ref.contains("/")) {
                UUID practId = UUID.fromString(ref.split("/")[1]);
                entity.setAuthor(practitionerRepository.findById(practId)
                        .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found: " + practId)));
            }
        }

        DocumentReferenceEntity saved = documentReferenceRepository.save(entity);
        return documentReferenceMapper.toFhirResource(saved);
    }

    @Override
    public DocumentReference update(UUID id, DocumentReference resource) {
        DocumentReferenceEntity existing = documentReferenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentReference not found: " + id));

        DocumentReferenceEntity updated = documentReferenceMapper.toEntity(resource);

        existing.setStatus(updated.getStatus());
        existing.setTypeCode(updated.getTypeCode());
        existing.setTypeDisplay(updated.getTypeDisplay());
        existing.setDescription(updated.getDescription());
        existing.setContentType(updated.getContentType());
        existing.setContentUrl(updated.getContentUrl());
        existing.setContentTitle(updated.getContentTitle());
        existing.setContentData(updated.getContentData());
        existing.setPeriodStart(updated.getPeriodStart());
        existing.setPeriodEnd(updated.getPeriodEnd());
        existing.setDate(updated.getDate());

        if (resource.hasSubject()) {
            String ref = resource.getSubject().getReference();
            if (ref != null && ref.contains("/")) {
                UUID patientId = UUID.fromString(ref.split("/")[1]);
                existing.setPatient(patientRepository.findById(patientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId)));
            }
        }

        if (resource.hasContext() && resource.getContext().hasEncounter()) {
            String ref = resource.getContext().getEncounterFirstRep().getReference();
            if (ref != null && ref.contains("/")) {
                UUID encId = UUID.fromString(ref.split("/")[1]);
                existing.setEncounter(encounterRepository.findById(encId)
                        .orElseThrow(() -> new ResourceNotFoundException("Encounter not found: " + encId)));
            }
        }

        if (resource.hasAuthor()) {
            String ref = resource.getAuthorFirstRep().getReference();
            if (ref != null && ref.contains("/")) {
                UUID practId = UUID.fromString(ref.split("/")[1]);
                existing.setAuthor(practitionerRepository.findById(practId)
                        .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found: " + practId)));
            }
        }

        DocumentReferenceEntity saved = documentReferenceRepository.save(existing);
        return documentReferenceMapper.toFhirResource(saved);
    }

    @Override
    public void delete(UUID id) {
        DocumentReferenceEntity entity = documentReferenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentReference not found: " + id));
        documentReferenceRepository.delete(entity);
    }
}
