package ehrAssist.service.impl;

import ehrAssist.dto.projection.RiskFeedProjection;
import ehrAssist.entity.ObservationEntity;
import ehrAssist.entity.VitalsEntity;
import ehrAssist.exception.ResourceNotFoundException;
import ehrAssist.mapper.ObservationMapper;
import ehrAssist.mapper.VitalsMapper;
import ehrAssist.repository.*;
import ehrAssist.repository.master.ObservationCodeMasterRepository;
import ehrAssist.service.ObservationService;
import ehrAssist.util.BundleBuilder;
import ehrAssist.util.FhirDateSearchParser;
import ehrAssist.util.FhirDateSearchParser.ParsedDateSearch;
import ehrAssist.util.FhirQuantitySearchParser;
import ehrAssist.util.FhirQuantitySearchParser.ParsedValueQuantity;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Observation.ObservationReferenceRangeComponent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

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
    private final VitalsRepository vitalsRepository;
    private final VitalsMapper vitalsMapper;

    @Override
    @Transactional(readOnly = true)
    public Observation getById(UUID id) {
        ObservationEntity entity = observationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Observation not found: " + id));
        return observationMapper.toFhirResource(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Bundle search(UUID id, UUID patientId, String code, String category, String valueQuantity,
                         List<String> dateParams, Pageable pageable) {
        ParsedValueQuantity parsedQuantity = FhirQuantitySearchParser.parse(valueQuantity);
        if (valueQuantity != null && !valueQuantity.isBlank() && parsedQuantity == null) {
            return bundleBuilder.searchSetWithPagination("Observation", List.of(), 0L, 0, pageable.getPageSize(), "");
        }

        List<ParsedDateSearch> parsedDates = new ArrayList<>();
        List<String> dateRawTerms = new ArrayList<>();
        if (dateParams != null) {
            for (String d : dateParams) {
                if (d == null || d.isBlank()) {
                    continue;
                }
                String trimmed = d.trim();
                ParsedDateSearch pd = FhirDateSearchParser.parse(trimmed);
                if (pd == null) {
                    return bundleBuilder.searchSetWithPagination("Observation", List.of(), 0L, 0, pageable.getPageSize(), "");
                }
                parsedDates.add(pd);
                dateRawTerms.add(trimmed);
            }
        }

        if (id == null && patientId == null && code == null && category == null
                && (valueQuantity == null || valueQuantity.isBlank())
                && parsedDates.isEmpty()) {
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
        for (ParsedDateSearch pd : parsedDates) {
            spec = spec.and((root, query, cb) -> buildObservationDatePredicate(root, cb, pd));
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
        for (String rawDate : dateRawTerms) {
            queryParams.append("date=").append(URLEncoder.encode(rawDate, StandardCharsets.UTF_8)).append("&");
        }
        String query = queryParams.length() > 0 ? queryParams.substring(0, queryParams.length() - 1) : "";

        return bundleBuilder.searchSetWithPagination("Observation", fhirResources, pageResult.getTotalElements(),
                pageable.getPageNumber(), pageable.getPageSize(), query);
    }

    /**
     * FHIR R4 {@code date} on Observation applies to {@code Observation.effective[x]} or {@code Observation.issued}
     * (see Observation search parameters). Match if either field satisfies the criterion.
     */
    private Predicate buildObservationDatePredicate(
            jakarta.persistence.criteria.Root<ObservationEntity> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            ParsedDateSearch parsed) {
        Path<LocalDateTime> eff = root.get("effectiveDate");
        Path<LocalDateTime> iss = root.get("issued");

        ParsedDateSearch compareAs = "ne".equals(parsed.prefix())
                ? new ParsedDateSearch("eq", parsed.instant(), parsed.datePrecision())
                : parsed;
        boolean negate = "ne".equals(parsed.prefix());

        Predicate onEff = cb.and(cb.isNotNull(eff), dateCompareOnPath(cb, eff, compareAs));
        Predicate onIss = cb.and(cb.isNotNull(iss), dateCompareOnPath(cb, iss, compareAs));
        Predicate positive = cb.or(onEff, onIss);

        if (negate) {
            return cb.and(cb.or(cb.isNotNull(eff), cb.isNotNull(iss)), cb.not(positive));
        }
        return positive;
    }

    private Predicate dateCompareOnPath(
            jakarta.persistence.criteria.CriteriaBuilder cb,
            Path<LocalDateTime> path,
            ParsedDateSearch p) {
        String prefix = p.prefix();
        LocalDateTime instant = p.instant();
        boolean datePrecision = p.datePrecision();
        LocalDateTime nextDayStart = instant.plusDays(1);

        if (datePrecision) {
            return switch (prefix) {
                case "eq", "ap" -> cb.and(
                        cb.greaterThanOrEqualTo(path, instant),
                        cb.lessThan(path, nextDayStart));
                case "gt", "sa" -> cb.greaterThanOrEqualTo(path, nextDayStart);
                case "ge" -> cb.greaterThanOrEqualTo(path, instant);
                case "lt", "eb" -> cb.lessThan(path, instant);
                case "le" -> cb.lessThan(path, nextDayStart);
                default -> cb.and(
                        cb.greaterThanOrEqualTo(path, instant),
                        cb.lessThan(path, nextDayStart));
            };
        }
        return switch (prefix) {
            case "eq", "ap" -> cb.equal(path, instant);
            case "gt", "sa" -> cb.greaterThan(path, instant);
            case "ge" -> cb.greaterThanOrEqualTo(path, instant);
            case "lt", "eb" -> cb.lessThan(path, instant);
            case "le" -> cb.lessThanOrEqualTo(path, instant);
            default -> cb.equal(path, instant);
        };
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

    @Override
    @Transactional(readOnly = true)
    public Bundle searchVitals(UUID patientId, String loincCode, Pageable pageable) {
        if (ObjectUtils.isEmpty(patientId) && ObjectUtils.isEmpty(loincCode)) {
            return bundleBuilder.searchSet("Observation", List.of(), 0);
        }

        Page<VitalsEntity> pageResult = vitalsRepository.searchVitals(patientId, loincCode, pageable);

        List<Resource> fhirResources = pageResult.getContent().stream()
                .map(vitalsMapper::toFhirResource)
                .map(Resource.class::cast)
                .toList();

        return bundleBuilder.searchSet("Observation", fhirResources, fhirResources.size());
    }

    private static final String UCUM_SYSTEM = "http://unitsofmeasure.org";

    @Override
    @Transactional(readOnly = true)
    public Bundle getRiskFeed(UUID practitionerId, LocalDate asOfDate) {

        // Default to today; window always covers the preceding 6 months
        LocalDate     effectiveAsOf = !ObjectUtils.isEmpty(asOfDate) ? asOfDate : LocalDate.now();
        LocalDateTime windowEnd     = effectiveAsOf.atTime(23, 59, 59);
        LocalDateTime windowStart   = effectiveAsOf.minusMonths(6).atStartOfDay();

        List<UUID> patientIds = patientRepository.findActivePatientIdsByPractitionerId(practitionerId);
        if (patientIds.isEmpty()) {
            return emptySearchSet();
        }

        List<RiskFeedProjection> rows = observationRepository.findRiskFeedByPatientIds(patientIds, windowStart, windowEnd);
        if (rows.isEmpty()) {
            return emptySearchSet();
        }
        Map<UUID, List<RiskFeedProjection>> grouped = rows.stream()
                .collect(Collectors.groupingBy(
                        RiskFeedProjection::getPatientId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        List<BundleEntryComponent> entries = grouped.entrySet().stream()
                .map(e -> {
                    List<RiskFeedProjection> patientRows = e.getValue();
                    List<Resource> containedObs = patientRows.stream()
                            .<Resource>map(this::buildObservation)
                            .toList();
                    return new BundleEntryComponent()
                            .setResource(buildPatientResource(e.getKey(), patientRows.get(0), containedObs));
                })
                .toList();

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setEntry(entries);
        bundle.setTotal(entries.size());
        return bundle;
    }

    private Bundle emptySearchSet() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal(0);
        return bundle;
    }

    private Patient buildPatientResource(UUID patientId,
                                         RiskFeedProjection demo,
                                         List<Resource> containedObs) {
        Patient patient = new Patient();
        patient.setId(patientId.toString());

        if (!ObjectUtils.isEmpty(demo.getFullName())) {
            String[] parts = demo.getFullName().trim().split(" ", 2);
            HumanName name = new HumanName().setUse(HumanName.NameUse.OFFICIAL);
            if (parts.length == 2) {
                name.setFamily(parts[1]).addGiven(parts[0]);
            } else {
                name.setFamily(demo.getFullName());
            }
            patient.addName(name);
        }

        if (!ObjectUtils.isEmpty(demo.getGender())) {
            patient.setGender(
                    org.hl7.fhir.r4.model.Enumerations.AdministrativeGender
                            .fromCode(demo.getGender().toLowerCase(Locale.ROOT))
            );
        }

        if (!ObjectUtils.isEmpty(demo.getPhone())) {
            patient.addTelecom()
                    .setSystem(org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem.PHONE)
                    .setValue(demo.getPhone());
        }

        if (!ObjectUtils.isEmpty(demo.getEmail())) {
            patient.addTelecom()
                    .setSystem(org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem.EMAIL)
                    .setValue(demo.getEmail());
        }

        patient.setContained(containedObs);
        return patient;
    }

    private Observation buildObservation(RiskFeedProjection row) {
        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);

        obs.setCode(new CodeableConcept().addCoding(
                new Coding()
                        .setSystem(row.getCodeSystem())
                        .setCode(row.getLoincCode())
                        .setDisplay(row.getDisplay())
        ));

        if (!ObjectUtils.isEmpty(row.getValueQuantity())) {
            obs.setValue(new Quantity()
                    .setValue(row.getValueQuantity())
                    .setUnit(row.getUnit())
                    .setSystem(UCUM_SYSTEM));
        }

        if (!ObjectUtils.isEmpty(row.getEffectiveDate())) {
            obs.setEffective(new DateTimeType(toDate(row.getEffectiveDate())));
        }

        ObservationReferenceRangeComponent refRange = new ObservationReferenceRangeComponent();
        if (!ObjectUtils.isEmpty(row.getReferenceRangeLow())
                && row.getReferenceRangeLow().compareTo(BigDecimal.ZERO) > 0) {
            refRange.setLow(new Quantity().setValue(row.getReferenceRangeLow()).setUnit(row.getUnit()));
        }
        if (!ObjectUtils.isEmpty(row.getReferenceRangeHigh())) {
            refRange.setHigh(new Quantity().setValue(row.getReferenceRangeHigh()).setUnit(row.getUnit()));
        }
        obs.addReferenceRange(refRange);

        return obs;
    }

    private Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.toInstant(ZoneOffset.UTC));
    }
}
