package ehrAssist.service.impl;

import ehrAssist.entity.EncounterEntity;
import ehrAssist.exception.ResourceNotFoundException;
import ehrAssist.mapper.EncounterMapper;
import ehrAssist.repository.EncounterRepository;
import ehrAssist.repository.PatientRepository;
import ehrAssist.repository.PractitionerRepository;
import ehrAssist.service.EncounterService;
import ehrAssist.util.BundleBuilder;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EncounterServiceImpl implements EncounterService {

    private final EncounterRepository encounterRepository;
    private final PatientRepository patientRepository;
    private final PractitionerRepository practitionerRepository;
    private final EncounterMapper encounterMapper;
    private final BundleBuilder bundleBuilder;

    @Override
    @Transactional(readOnly = true)
    public Encounter getById(UUID id) {
        EncounterEntity entity = encounterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found: " + id));
        return encounterMapper.toFhirResource(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Bundle search(UUID id, UUID patientId, String status, String encounterClass, List<String> dateParams, Pageable pageable) {
        if (id == null && patientId == null && status == null && encounterClass == null && (dateParams == null || dateParams.isEmpty())) {
            return bundleBuilder.searchSetWithPagination("Encounter", List.of(), 0L, 0, pageable.getPageSize(), "");
        }

        Specification<EncounterEntity> spec = Specification.where(null);

        if (id != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("id"), id));
        }
        if (patientId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("patient").get("id"), patientId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (encounterClass != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("encounterClass"), encounterClass));
        }

        // FHIR R4 date search parameter with prefixes - handle multiple date parameters
        if (dateParams != null && !dateParams.isEmpty()) {
            for (String dateParam : dateParams) {
                spec = spec.and(parseDateSearchParameter(dateParam));
            }
        }

        Page<EncounterEntity> pageResult = encounterRepository.findAll(spec, pageable);

        List<Resource> fhirResources = pageResult.getContent().stream()
                .map(encounterMapper::toFhirResource)
                .map(Resource.class::cast)
                .toList();

        StringBuilder queryParams = new StringBuilder();
        if (id != null) queryParams.append("_id=").append(id).append("&");
        if (patientId != null) queryParams.append("patient=").append(patientId).append("&");
        if (status != null) queryParams.append("status=").append(status).append("&");
        if (encounterClass != null) queryParams.append("class=").append(encounterClass).append("&");
        if (dateParams != null && !dateParams.isEmpty()) {
            for (String dateParam : dateParams) {
                queryParams.append("date=").append(dateParam).append("&");
            }
        }
        String query = queryParams.length() > 0 ? queryParams.substring(0, queryParams.length() - 1) : "";

        return bundleBuilder.searchSetWithPagination("Encounter", fhirResources, pageResult.getTotalElements(),
                pageable.getPageNumber(), pageable.getPageSize(), query);
    }

    /**
     * Parse FHIR date search parameter with prefixes according to FHIR R4 specification
     * Supports: eq, ne, gt, lt, ge, le, sa, eb, ap
     * <p>
     * This implementation searches for encounters that are ACTIVE/OVERLAPPING during the specified date/range
     * An encounter overlaps if: periodStart <= searchEnd AND (periodEnd >= searchStart OR periodEnd is null)
     * <p>
     * Examples:
     * - date=2024-03-20 (encounters active on this date)
     * - date=ge2024-01-01 (encounters active on or after this date)
     * - date=lt2024-12-31 (encounters that started before this date)
     */
    private Specification<EncounterEntity> parseDateSearchParameter(String dateParam) {
        return (root, query, cb) -> {
            String prefix = "eq";
            String dateValue = dateParam;

            // Extract prefix if present
            if (dateParam.length() > 2) {
                String possiblePrefix = dateParam.substring(0, 2).toLowerCase();
                if (List.of("eq", "ne", "gt", "lt", "ge", "le", "sa", "eb", "ap").contains(possiblePrefix)) {
                    prefix = possiblePrefix;
                    dateValue = dateParam.substring(2);
                }
            }

            try {
                LocalDateTime searchDateTime = parseDate(dateValue);
                LocalDateTime endOfDay = searchDateTime.toLocalDate().atTime(LocalTime.MAX);

                return switch (prefix) {
                    case "eq" -> {
                        // Encounters active on this specific date
                        // periodStart <= endOfDay AND (periodEnd >= searchDateTime OR periodEnd is null)
                        yield cb.and(
                                cb.lessThanOrEqualTo(root.get("periodStart"), endOfDay),
                                cb.or(
                                        cb.greaterThanOrEqualTo(root.get("periodEnd"), searchDateTime),
                                        cb.isNull(root.get("periodEnd"))
                                )
                        );
                    }
                    case "ne" -> {
                        // Encounters NOT active on this date
                        yield cb.or(
                                cb.greaterThan(root.get("periodStart"), endOfDay),
                                cb.and(
                                        cb.isNotNull(root.get("periodEnd")),
                                        cb.lessThan(root.get("periodEnd"), searchDateTime)
                                )
                        );
                    }
                    case "gt", "sa" -> {
                        // Encounters that start after this date
                        yield cb.greaterThan(root.get("periodStart"), endOfDay);
                    }
                    case "lt", "eb" -> {
                        // Encounters that end before this date (or started before if no end date)
                        yield cb.or(
                                cb.and(
                                        cb.isNotNull(root.get("periodEnd")),
                                        cb.lessThan(root.get("periodEnd"), searchDateTime)
                                ),
                                cb.and(
                                        cb.isNull(root.get("periodEnd")),
                                        cb.lessThan(root.get("periodStart"), searchDateTime)
                                )
                        );
                    }
                    case "ge" -> {
                        // Encounters active on or after this date
                        // periodStart >= searchDateTime OR (periodEnd >= searchDateTime OR periodEnd is null)
                        yield cb.or(
                                cb.greaterThanOrEqualTo(root.get("periodStart"), searchDateTime),
                                cb.greaterThanOrEqualTo(root.get("periodEnd"), searchDateTime),
                                cb.isNull(root.get("periodEnd"))
                        );
                    }
                    case "le" -> {
                        // Encounters that started on or before this date
                        yield cb.lessThanOrEqualTo(root.get("periodStart"), endOfDay);
                    }
                    case "ap" -> {
                        // Approximately - encounters active within ±1 day
                        LocalDateTime minusOne = searchDateTime.minusDays(1);
                        LocalDateTime plusOne = endOfDay.plusDays(1);
                        yield cb.and(
                                cb.lessThanOrEqualTo(root.get("periodStart"), plusOne),
                                cb.or(
                                        cb.greaterThanOrEqualTo(root.get("periodEnd"), minusOne),
                                        cb.isNull(root.get("periodEnd"))
                                )
                        );
                    }
                    default -> {
                        // Default to eq behavior
                        yield cb.and(
                                cb.lessThanOrEqualTo(root.get("periodStart"), endOfDay),
                                cb.or(
                                        cb.greaterThanOrEqualTo(root.get("periodEnd"), searchDateTime),
                                        cb.isNull(root.get("periodEnd"))
                                )
                        );
                    }
                };
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date format: " + dateValue + ". Expected format: YYYY-MM-DD");
            }
        };
    }

    /**
     * Parse date string to LocalDateTime
     * Supports: YYYY, YYYY-MM, YYYY-MM-DD formats
     */
    private LocalDateTime parseDate(String dateStr) {
        if (dateStr.length() == 4) {
            // Year only: YYYY
            return LocalDate.parse(dateStr + "-01-01", DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } else if (dateStr.length() == 7) {
            // Year-Month: YYYY-MM
            return LocalDate.parse(dateStr + "-01", DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } else {
            // Full date: YYYY-MM-DD
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        }
    }

    @Override
    public Encounter create(Encounter resource) {
        EncounterEntity entity = encounterMapper.toEntity(resource);

        if (resource.hasSubject()) {
            String ref = resource.getSubject().getReference();
            if (ref != null && ref.contains("/")) {
                UUID patientId = UUID.fromString(ref.split("/")[1]);
                entity.setPatient(patientRepository.findById(patientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId)));
            }
        }

        if (resource.hasParticipant()) {
            for (var participant : resource.getParticipant()) {
                if (participant.hasIndividual()) {
                    String ref = participant.getIndividual().getReference();
                    if (ref != null && ref.startsWith("Practitioner/")) {
                        UUID practId = UUID.fromString(ref.split("/")[1]);
                        entity.setPractitioner(practitionerRepository.findById(practId)
                                .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found: " + practId)));
                        break;
                    }
                }
            }
        }

        EncounterEntity saved = encounterRepository.save(entity);
        return encounterMapper.toFhirResource(saved);
    }

    @Override
    public Encounter update(UUID id, Encounter resource) {
        EncounterEntity existing = encounterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found: " + id));

        EncounterEntity updated = encounterMapper.toEntity(resource);

        existing.setStatus(updated.getStatus());
        existing.setEncounterClass(updated.getEncounterClass());
        existing.setTypeCode(updated.getTypeCode());
        existing.setTypeDisplay(updated.getTypeDisplay());
        existing.setPeriodStart(updated.getPeriodStart());
        existing.setPeriodEnd(updated.getPeriodEnd());
        existing.setAdmissionLocation(updated.getAdmissionLocation());
        existing.setDischargeLocation(updated.getDischargeLocation());
        existing.setDischargeDispositionCode(updated.getDischargeDispositionCode());
        existing.setReasonCode(updated.getReasonCode());
        existing.setReasonDisplay(updated.getReasonDisplay());
        existing.setDiagnosisText(updated.getDiagnosisText());
        existing.setInsurance(updated.getInsurance());
        existing.setClinicalNotes(updated.getClinicalNotes());

        if (resource.hasSubject()) {
            String ref = resource.getSubject().getReference();
            if (ref != null && ref.contains("/")) {
                UUID patientId = UUID.fromString(ref.split("/")[1]);
                existing.setPatient(patientRepository.findById(patientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId)));
            }
        }

        if (resource.hasParticipant()) {
            for (var participant : resource.getParticipant()) {
                if (participant.hasIndividual()) {
                    String ref = participant.getIndividual().getReference();
                    if (ref != null && ref.startsWith("Practitioner/")) {
                        UUID practId = UUID.fromString(ref.split("/")[1]);
                        existing.setPractitioner(practitionerRepository.findById(practId)
                                .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found: " + practId)));
                        break;
                    }
                }
            }
        }

        EncounterEntity saved = encounterRepository.save(existing);
        return encounterMapper.toFhirResource(saved);
    }

    @Override
    public void delete(UUID id) {
        EncounterEntity entity = encounterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found: " + id));
        encounterRepository.delete(entity);
    }
}
