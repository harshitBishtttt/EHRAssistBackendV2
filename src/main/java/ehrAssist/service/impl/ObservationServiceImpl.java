package ehrAssist.service.impl;

import ehrAssist.entity.ObservationEntity;
import ehrAssist.exception.ResourceNotFoundException;
import ehrAssist.mapper.ObservationMapper;
import ehrAssist.repository.EncounterRepository;
import ehrAssist.repository.ObservationRepository;
import ehrAssist.repository.PatientRepository;
import ehrAssist.repository.PractitionerRepository;
import ehrAssist.repository.master.ObservationCodeMasterRepository;
import ehrAssist.service.ObservationService;
import ehrAssist.util.BundleBuilder;
import ehrAssist.util.FhirQuantitySearchParser;
import ehrAssist.util.FhirQuantitySearchParser.ParsedValueQuantity;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ObservationServiceImpl implements ObservationService {

    private final ObservationRepository observationRepository;
    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final PractitionerRepository practitionerRepository;
    private final ObservationCodeMasterRepository observationCodeMasterRepository;
    private final ObservationMapper observationMapper;
    private final BundleBuilder bundleBuilder;

    @Override
    @Transactional(readOnly = true)
    public Observation getById(UUID id) {
        ObservationEntity entity = observationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Observation not found: " + id));
        return observationMapper.toFhirResource(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Bundle search(UUID id, UUID patientId, String code, String category, String valueQuantity, Pageable pageable) {
        ParsedValueQuantity parsedQuantity = FhirQuantitySearchParser.parse(valueQuantity);
        if (valueQuantity != null && !valueQuantity.isBlank() && parsedQuantity == null) {
            return bundleBuilder.searchSetWithPagination("Observation", List.of(), 0L, 0, pageable.getPageSize(), "");
        }

        if (id == null && patientId == null && code == null && category == null
                && (valueQuantity == null || valueQuantity.isBlank())) {
            return bundleBuilder.searchSetWithPagination("Observation", List.of(), 0L, 0, pageable.getPageSize(), "");
        }

        Specification<ObservationEntity> spec = Specification.where(null);

        if (id != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("id"), id));
        }
        if (patientId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("patient").get("id"), patientId));
        }
        if (code != null) {
            spec = spec.and((root, query, cb) -> {
                var codeJoin = root.join("codeMaster");
                return cb.equal(codeJoin.get("loincCode"), code);
            });
        }
        if (category != null) {
            spec = spec.and((root, query, cb) -> {
                var codeJoin = root.join("codeMaster");
                return cb.equal(codeJoin.get("fhirCategoryCode"), category);
            });
        }
        if (parsedQuantity != null) {
            spec = spec.and((root, query, cb) -> buildQuantityPredicates(root, cb, parsedQuantity));
        }

        Page<ObservationEntity> pageResult = observationRepository.findAll(spec, pageable);
        
        List<Resource> fhirResources = pageResult.getContent().stream()
                .map(observationMapper::toFhirResource)
                .map(Resource.class::cast)
                .toList();

        StringBuilder queryParams = new StringBuilder();
        if (id != null) queryParams.append("_id=").append(id).append("&");
        if (patientId != null) queryParams.append("patient=").append(patientId).append("&");
        if (code != null) queryParams.append("code=").append(code).append("&");
        if (category != null) queryParams.append("category=").append(category).append("&");
        if (valueQuantity != null && !valueQuantity.isBlank()) {
            queryParams.append("value-quantity=")
                    .append(URLEncoder.encode(valueQuantity, StandardCharsets.UTF_8))
                    .append("&");
        }
        String query = queryParams.length() > 0 ? queryParams.substring(0, queryParams.length() - 1) : "";

        return bundleBuilder.searchSetWithPagination("Observation", fhirResources, pageResult.getTotalElements(), 
                pageable.getPageNumber(), pageable.getPageSize(), query);
    }

    private Predicate buildQuantityPredicates(
            jakarta.persistence.criteria.Root<ObservationEntity> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            ParsedValueQuantity vq) {
        jakarta.persistence.criteria.Path<BigDecimal> qtyPath = root.<BigDecimal>get("valueQuantity");
        List<Predicate> parts = new ArrayList<>();
        parts.add(cb.isNotNull(qtyPath));
        parts.add(quantityCompare(cb, qtyPath, vq));
        if (vq.hasUnit()) {
            jakarta.persistence.criteria.Path<String> unitPath = root.<String>get("valueUnit");
            parts.add(cb.equal(cb.lower(unitPath), vq.unit().trim().toLowerCase(Locale.ROOT)));
        }
        return cb.and(parts.toArray(Predicate[]::new));
    }

    private Predicate quantityCompare(
            jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Path<BigDecimal> qtyPath,
            ParsedValueQuantity vq) {
        BigDecimal n = vq.number();
        return switch (vq.prefix()) {
            case "eq" -> cb.equal(qtyPath, n);
            case "ne" -> cb.notEqual(qtyPath, n);
            case "gt" -> cb.greaterThan(qtyPath, n);
            case "lt" -> cb.lessThan(qtyPath, n);
            case "ge" -> cb.greaterThanOrEqualTo(qtyPath, n);
            case "le" -> cb.lessThanOrEqualTo(qtyPath, n);
            case "sa" -> cb.greaterThanOrEqualTo(qtyPath, n);
            case "eb" -> cb.lessThanOrEqualTo(qtyPath, n);
            default -> cb.equal(qtyPath, n);
        };
    }

    @Override
    public Observation create(Observation resource) {
        ObservationEntity entity = observationMapper.toEntity(resource);

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

        if (resource.hasCode() && resource.getCode().hasCoding()) {
            Coding coding = resource.getCode().getCodingFirstRep();
            observationCodeMasterRepository.findByCodeSystemAndLoincCode(coding.getSystem(), coding.getCode())
                    .ifPresent(entity::setCodeMaster);
        }

        ObservationEntity saved = observationRepository.save(entity);
        return observationMapper.toFhirResource(saved);
    }

    @Override
    public Observation update(UUID id, Observation resource) {
        ObservationEntity existing = observationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Observation not found: " + id));

        ObservationEntity updated = observationMapper.toEntity(resource);

        existing.setStatus(updated.getStatus());
        existing.setValueQuantity(updated.getValueQuantity());
        existing.setValueUnit(updated.getValueUnit());
        existing.setValueString(updated.getValueString());
        existing.setInterpretationCode(updated.getInterpretationCode());
        existing.setEffectiveDate(updated.getEffectiveDate());
        existing.setIssued(updated.getIssued());

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

        if (resource.hasCode() && resource.getCode().hasCoding()) {
            Coding coding = resource.getCode().getCodingFirstRep();
            observationCodeMasterRepository.findByCodeSystemAndLoincCode(coding.getSystem(), coding.getCode())
                    .ifPresent(existing::setCodeMaster);
        }

        ObservationEntity saved = observationRepository.save(existing);
        return observationMapper.toFhirResource(saved);
    }

    @Override
    public void delete(UUID id) {
        ObservationEntity entity = observationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Observation not found: " + id));
        observationRepository.delete(entity);
    }
}
