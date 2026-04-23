package ehrAssist.service.impl;

import ehrAssist.entity.AppointmentEntity;
import ehrAssist.entity.ConditionEntity;
import ehrAssist.entity.MedicationRequestEntity;
import ehrAssist.entity.ObservationEntity;
import ehrAssist.entity.PatientEntity;
import ehrAssist.entity.ServiceRequestEntity;
import ehrAssist.exception.FhirValidationException;
import ehrAssist.exception.ResourceNotFoundException;
import ehrAssist.repository.AppointmentRepository;
import ehrAssist.repository.ConditionRepository;
import ehrAssist.repository.MedicationRequestRepository;
import ehrAssist.repository.OrganizationRepository;
import ehrAssist.repository.ObservationRepository;
import ehrAssist.repository.PatientRepository;
import ehrAssist.repository.PractitionerRepository;
import ehrAssist.repository.ServiceRequestRepository;
import ehrAssist.service.CareGapService;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareGapServiceImpl implements CareGapService {

    private static final String DIABETES_MEASURE_URL = "http://ehrassist.com/fhir/Measure/diabetes-hba1c-followup-gap";
    private static final String HYPERTENSION_MEASURE_URL = "http://ehrassist.com/fhir/Measure/hypertension-bp-followup-gap";
    private static final String CKD_MEASURE_URL = "http://ehrassist.com/fhir/Measure/ckd-creatinine-followup-gap";
    private static final String KIDNEY_CANCER_MEASURE_URL = "http://ehrassist.com/fhir/Measure/kidney-cancer-surveillance-gap";
    private static final String GAP_STATUS_URL = "http://ehrassist.com/fhir/StructureDefinition/care-gap-status";
    private static final String GAP_DETAIL_URL = "http://ehrassist.com/fhir/StructureDefinition/care-gap-detail";
    private static final String OPEN_GAP = "open-gap";
    private static final String CLOSED_GAP = "closed-gap";
    private static final String ALL = "all";

    private final PatientRepository patientRepository;
    private final PractitionerRepository practitionerRepository;
    private final OrganizationRepository organizationRepository;
    private final ConditionRepository conditionRepository;
    private final ObservationRepository observationRepository;
    private final AppointmentRepository appointmentRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final MedicationRequestRepository medicationRequestRepository;

    @Override
    public Bundle evaluateDiabetesCareGaps(LocalDate periodStart,
                                           LocalDate periodEnd,
                                           String subject,
                                           String practitioner,
                                           String organization,
                                           String status) {
        validateRequest(periodStart, periodEnd, subject, practitioner, organization, status);

        UUID subjectId = parseReference(subject, "Patient");
        UUID practitionerId = parseReference(practitioner, "Practitioner");
        UUID organizationId = parseReference(organization, "Organization");

        List<PatientEntity> patients = resolvePatients(subjectId, practitionerId, organizationId);
        List<UUID> patientIds = patients.stream().map(PatientEntity::getId).toList();
        String normalizedStatus = status.trim().toLowerCase(Locale.ROOT);

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setTimestamp(new Date());

        if (patientIds.isEmpty()) {
            bundle.setTotal(0);
            return bundle;
        }

        Map<UUID, List<ConditionEntity>> conditionsByPatientId = groupByPatientId(
                conditionRepository.findAllByPatientIdInWithCodeMaster(patientIds),
                ConditionEntity::getPatient
        );
        Map<UUID, List<ObservationEntity>> observationsByPatientId = groupByPatientId(
                observationRepository.findAllByPatientIdInWithCodeMaster(patientIds),
                ObservationEntity::getPatient
        );
        Map<UUID, List<ServiceRequestEntity>> serviceRequestsByPatientId = groupByPatientId(
                serviceRequestRepository.findByPatientIdIn(patientIds),
                ServiceRequestEntity::getPatient
        );
        Map<UUID, List<AppointmentEntity>> appointmentsByPatientId = groupByPatientId(
                appointmentRepository.findByPatientIdIn(patientIds),
                AppointmentEntity::getPatient
        );
        Map<UUID, List<MedicationRequestEntity>> medicationsByPatientId = groupByPatientId(
                medicationRequestRepository.findAllByPatientIdInWithMedicationCode(patientIds),
                MedicationRequestEntity::getPatient
        );

        patients.stream()
                .flatMap(patient -> evaluatePatient(
                        patient,
                        conditionsByPatientId.getOrDefault(patient.getId(), List.of()),
                        observationsByPatientId.getOrDefault(patient.getId(), List.of()),
                        serviceRequestsByPatientId.getOrDefault(patient.getId(), List.of()),
                        appointmentsByPatientId.getOrDefault(patient.getId(), List.of()),
                        medicationsByPatientId.getOrDefault(patient.getId(), List.of()),
                        periodStart,
                        periodEnd
                ).stream())
                .filter(evaluation -> matchesStatus(evaluation.gapStatus(), normalizedStatus))
                .map(evaluation -> toMeasureReport(evaluation, periodStart, periodEnd))
                .forEach(report -> bundle.addEntry()
                        .setFullUrl(report.getIdElement().getValue())
                        .setResource(report));

        bundle.setTotal(bundle.getEntry().size());
        return bundle;
    }

    private void validateRequest(LocalDate periodStart,
                                 LocalDate periodEnd,
                                 String subject,
                                 String practitioner,
                                 String organization,
                                 String status) {
        if (periodStart == null || periodEnd == null) {
            throw new FhirValidationException("periodStart and periodEnd are required.");
        }
        if (periodStart.isAfter(periodEnd)) {
            throw new FhirValidationException("periodStart must be before or equal to periodEnd.");
        }

        int scopeCount = Stream.of(subject, practitioner, organization)
                .filter(value -> value != null && !value.isBlank())
                .toArray().length;
        if (scopeCount != 1) {
            throw new FhirValidationException("Exactly one of subject, practitioner, or organization must be provided.");
        }

        String normalizedStatus = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        if (!List.of(OPEN_GAP, CLOSED_GAP, ALL).contains(normalizedStatus)) {
            throw new FhirValidationException("status must be one of open-gap, closed-gap, or all.");
        }
    }

    private UUID parseReference(String value, String resourceType) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();
        String idPart = trimmed;
        String prefix = resourceType + "/";
        if (trimmed.startsWith(prefix)) {
            idPart = trimmed.substring(prefix.length());
        }

        try {
            return UUID.fromString(idPart);
        } catch (IllegalArgumentException ex) {
            throw new FhirValidationException(resourceType + " reference must be a UUID or " + prefix + "{uuid}.");
        }
    }

    private List<PatientEntity> resolvePatients(UUID subjectId, UUID practitionerId, UUID organizationId) {
        if (subjectId != null) {
            PatientEntity patient = patientRepository.findWithNamesById(subjectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + subjectId));
            return List.of(patient);
        }
        if (practitionerId != null) {
            practitionerRepository.findById(practitionerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found: " + practitionerId));
            return patientRepository.findWithNamesByPrimaryPractitionerId(practitionerId);
        }

        organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        return patientRepository.findWithNamesByManagingOrganizationId(organizationId);
    }

    private List<GapEvaluation> evaluatePatient(PatientEntity patient,
                                                List<ConditionEntity> conditions,
                                                List<ObservationEntity> observations,
                                                List<ServiceRequestEntity> serviceRequests,
                                                List<AppointmentEntity> appointments,
                                                List<MedicationRequestEntity> medications,
                                                LocalDate periodStart,
                                                LocalDate periodEnd) {
        List<GapEvaluation> evaluations = new ArrayList<>();

        firstMatchingCondition(conditions, this::isActiveDiabetesCondition)
                .map(condition -> evaluateDiabetesGap(patient, condition, observations, serviceRequests, appointments, medications, periodStart, periodEnd))
                .ifPresent(evaluations::add);

        firstMatchingCondition(conditions, this::isActiveHypertensionCondition)
                .map(condition -> evaluateHypertensionGap(patient, condition, observations, serviceRequests, appointments, medications, periodStart, periodEnd))
                .ifPresent(evaluations::add);

        firstMatchingCondition(conditions, this::isActiveCkdCondition)
                .map(condition -> evaluateCkdGap(patient, condition, observations, serviceRequests, appointments, medications, periodStart, periodEnd))
                .ifPresent(evaluations::add);

        firstMatchingCondition(conditions, this::isActiveKidneyCancerCondition)
                .map(condition -> evaluateKidneyCancerGap(patient, condition, observations, serviceRequests, appointments, medications, periodStart, periodEnd))
                .ifPresent(evaluations::add);

        return evaluations;
    }

    private <T> Map<UUID, List<T>> groupByPatientId(List<T> resources, Function<T, PatientEntity> patientExtractor) {
        if (resources == null || resources.isEmpty()) {
            return Collections.emptyMap();
        }
        return resources.stream()
                .filter(resource -> patientExtractor.apply(resource) != null)
                .collect(Collectors.groupingBy(resource -> patientExtractor.apply(resource).getId()));
    }

    private java.util.Optional<ConditionEntity> firstMatchingCondition(List<ConditionEntity> conditions, Predicate<ConditionEntity> predicate) {
        return conditions.stream().filter(predicate).findFirst();
    }

    private GapEvaluation evaluateDiabetesGap(PatientEntity patient,
                                              ConditionEntity condition,
                                              List<ObservationEntity> observations,
                                              List<ServiceRequestEntity> serviceRequests,
                                              List<AppointmentEntity> appointments,
                                              List<MedicationRequestEntity> medications,
                                              LocalDate periodStart,
                                              LocalDate periodEnd) {
        ObservationEntity latestInPeriod = latestObservationInPeriod(observations, this::isHemoglobinA1c, periodStart, periodEnd);
        ObservationEntity latestAbnormalBefore = latestAbnormalObservationBeforePeriod(observations, this::isHemoglobinA1c, this::isAbnormalA1c, periodStart);
        ServiceRequestEntity latestOrder = latestServiceRequest(serviceRequests, this::isA1cServiceRequest, periodEnd);
        AppointmentEntity latestMissedFollowUp = latestMissedAppointment(appointments, this::isDiabetesFollowUpAppointment, periodStart, periodEnd);
        boolean hasMedication = medications.stream().anyMatch(this::isActiveDiabetesMedication);

        return new GapEvaluation(
                patient,
                GapType.DIABETES_HBA1C,
                latestInPeriod == null ? OPEN_GAP : CLOSED_GAP,
                condition,
                latestAbnormalBefore,
                latestInPeriod,
                latestOrder,
                null,
                latestMissedFollowUp,
                hasMedication
        );
    }

    private GapEvaluation evaluateHypertensionGap(PatientEntity patient,
                                                  ConditionEntity condition,
                                                  List<ObservationEntity> observations,
                                                  List<ServiceRequestEntity> serviceRequests,
                                                  List<AppointmentEntity> appointments,
                                                  List<MedicationRequestEntity> medications,
                                                  LocalDate periodStart,
                                                  LocalDate periodEnd) {
        ObservationEntity latestInPeriod = latestObservationInPeriod(observations, this::isBloodPressureObservation, periodStart, periodEnd);
        ObservationEntity latestAbnormalBefore = latestAbnormalObservationBeforePeriod(observations, this::isBloodPressureObservation, this::isAbnormalBloodPressure, periodStart);
        ServiceRequestEntity latestOrder = latestServiceRequest(serviceRequests, this::isHypertensionServiceRequest, periodEnd);
        AppointmentEntity latestMissedFollowUp = latestMissedAppointment(appointments, this::isHypertensionFollowUpAppointment, periodStart, periodEnd);
        boolean hasMedication = medications.stream().anyMatch(this::isActiveHypertensionMedication);

        return new GapEvaluation(
                patient,
                GapType.HYPERTENSION_BP,
                latestInPeriod == null ? OPEN_GAP : CLOSED_GAP,
                condition,
                latestAbnormalBefore,
                latestInPeriod,
                latestOrder,
                null,
                latestMissedFollowUp,
                hasMedication
        );
    }

    private GapEvaluation evaluateCkdGap(PatientEntity patient,
                                         ConditionEntity condition,
                                         List<ObservationEntity> observations,
                                         List<ServiceRequestEntity> serviceRequests,
                                         List<AppointmentEntity> appointments,
                                         List<MedicationRequestEntity> medications,
                                         LocalDate periodStart,
                                         LocalDate periodEnd) {
        ObservationEntity latestInPeriod = latestObservationInPeriod(observations, this::isCreatinineObservation, periodStart, periodEnd);
        ObservationEntity latestAbnormalBefore = latestAbnormalObservationBeforePeriod(observations, this::isCreatinineObservation, this::isAbnormalCreatinine, periodStart);
        ServiceRequestEntity latestOrder = latestServiceRequest(serviceRequests, this::isCreatinineServiceRequest, periodEnd);
        AppointmentEntity latestMissedFollowUp = latestMissedAppointment(appointments, this::isCkdFollowUpAppointment, periodStart, periodEnd);
        boolean hasMedication = medications.stream().anyMatch(this::isActiveCkdMedication);

        return new GapEvaluation(
                patient,
                GapType.CKD_CREATININE,
                latestInPeriod == null ? OPEN_GAP : CLOSED_GAP,
                condition,
                latestAbnormalBefore,
                latestInPeriod,
                latestOrder,
                null,
                latestMissedFollowUp,
                hasMedication
        );
    }

    private GapEvaluation evaluateKidneyCancerGap(PatientEntity patient,
                                                  ConditionEntity condition,
                                                  List<ObservationEntity> observations,
                                                  List<ServiceRequestEntity> serviceRequests,
                                                  List<AppointmentEntity> appointments,
                                                  List<MedicationRequestEntity> medications,
                                                  LocalDate periodStart,
                                                  LocalDate periodEnd) {
        ObservationEntity latestSupportingObservation = latestAbnormalObservationBeforePeriod(observations, this::isCreatinineObservation, this::isAbnormalCreatinine, periodStart);
        ServiceRequestEntity latestOrder = latestServiceRequest(serviceRequests, this::isKidneyCancerServiceRequest, periodEnd);
        AppointmentEntity completedFollowUp = latestCompletedAppointment(appointments, this::isKidneyCancerFollowUpAppointment, periodStart, periodEnd);
        AppointmentEntity latestMissedFollowUp = latestMissedAppointment(appointments, this::isKidneyCancerFollowUpAppointment, periodStart, periodEnd);

        return new GapEvaluation(
                patient,
                GapType.KIDNEY_CANCER_SURVEILLANCE,
                completedFollowUp == null ? OPEN_GAP : CLOSED_GAP,
                condition,
                latestSupportingObservation,
                null,
                latestOrder,
                completedFollowUp,
                latestMissedFollowUp,
                false
        );
    }

    private boolean isActiveDiabetesCondition(ConditionEntity condition) {
        return isActiveCondition(condition) && (
                codeStartsWith(condition, "E10", "E11", "E13")
                        || titleContains(condition, "diabet")
        );
    }

    private boolean isActiveHypertensionCondition(ConditionEntity condition) {
        return isActiveCondition(condition) && (
                codeStartsWith(condition, "I10", "I11", "I12", "I13", "I15")
                        || titleContains(condition, "hypertension")
                        || titleContains(condition, "hypertensive")
        );
    }

    private boolean isActiveCkdCondition(ConditionEntity condition) {
        return isActiveCondition(condition) && (
                codeStartsWith(condition, "N18")
                        || titleContains(condition, "chronic kidney disease")
                        || titleContains(condition, "ckd")
        );
    }

    private boolean isActiveKidneyCancerCondition(ConditionEntity condition) {
        return isActiveCondition(condition) && (
                codeStartsWith(condition, "C64")
                        || (titleContains(condition, "kidney") && (titleContains(condition, "malignant") || titleContains(condition, "cancer") || titleContains(condition, "neoplasm")))
        );
    }

    private boolean isActiveCondition(ConditionEntity condition) {
        if (condition == null || condition.getCodeMaster() == null) {
            return false;
        }
        String clinicalStatus = safeLower(condition.getClinicalStatus());
        return !clinicalStatus.contains("inactive")
                && !clinicalStatus.contains("resolved")
                && !clinicalStatus.contains("remission")
                && condition.getAbatementDate() == null;
    }

    private boolean codeStartsWith(ConditionEntity condition, String... prefixes) {
        String icd10 = safeUpper(condition.getCodeMaster().getIcd10Code());
        for (String prefix : prefixes) {
            if (icd10.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean titleContains(ConditionEntity condition, String token) {
        String shortTitle = safeLower(condition.getCodeMaster().getShortTitle());
        String longTitle = safeLower(condition.getCodeMaster().getLongTitle());
        return shortTitle.contains(token) || longTitle.contains(token);
    }

    private ObservationEntity latestObservationInPeriod(List<ObservationEntity> observations,
                                                        Predicate<ObservationEntity> predicate,
                                                        LocalDate periodStart,
                                                        LocalDate periodEnd) {
        return observations.stream()
                .filter(predicate)
                .filter(observation -> isWithinPeriod(observation.getEffectiveDate(), periodStart, periodEnd))
                .max(Comparator.comparing(ObservationEntity::getEffectiveDate))
                .orElse(null);
    }

    private ObservationEntity latestAbnormalObservationBeforePeriod(List<ObservationEntity> observations,
                                                                    Predicate<ObservationEntity> typePredicate,
                                                                    Predicate<ObservationEntity> abnormalPredicate,
                                                                    LocalDate periodStart) {
        return observations.stream()
                .filter(typePredicate)
                .filter(observation -> observation.getEffectiveDate() != null)
                .filter(observation -> observation.getEffectiveDate().toLocalDate().isBefore(periodStart))
                .filter(abnormalPredicate)
                .max(Comparator.comparing(ObservationEntity::getEffectiveDate))
                .orElse(null);
    }

    private ServiceRequestEntity latestServiceRequest(List<ServiceRequestEntity> requests,
                                                      Predicate<ServiceRequestEntity> predicate,
                                                      LocalDate periodEnd) {
        return requests.stream()
                .filter(predicate)
                .filter(request -> request.getAuthoredOn() == null || !request.getAuthoredOn().toLocalDate().isAfter(periodEnd))
                .max(Comparator.comparing(request -> request.getAuthoredOn() != null ? request.getAuthoredOn() : LocalDateTime.MIN))
                .orElse(null);
    }

    private AppointmentEntity latestMissedAppointment(List<AppointmentEntity> appointments,
                                                      Predicate<AppointmentEntity> predicate,
                                                      LocalDate periodStart,
                                                      LocalDate periodEnd) {
        return appointments.stream()
                .filter(predicate)
                .filter(appointment -> safeLower(appointment.getStatus()).equals("noshow"))
                .filter(appointment -> isWithinPeriod(appointment.getStartTime(), periodStart, periodEnd))
                .max(Comparator.comparing(AppointmentEntity::getStartTime))
                .orElse(null);
    }

    private AppointmentEntity latestCompletedAppointment(List<AppointmentEntity> appointments,
                                                         Predicate<AppointmentEntity> predicate,
                                                         LocalDate periodStart,
                                                         LocalDate periodEnd) {
        return appointments.stream()
                .filter(predicate)
                .filter(appointment -> safeLower(appointment.getStatus()).equals("fulfilled"))
                .filter(appointment -> isWithinPeriod(appointment.getStartTime(), periodStart, periodEnd))
                .max(Comparator.comparing(AppointmentEntity::getStartTime))
                .orElse(null);
    }

    private boolean isHemoglobinA1c(ObservationEntity observation) {
        if (observation == null || observation.getCodeMaster() == null) {
            return false;
        }
        String loinc = safeUpper(observation.getCodeMaster().getLoincCode());
        String display = safeLower(observation.getCodeMaster().getCodeDisplay());
        return loinc.equals("4548-4") || loinc.equals("17856-6") || display.contains("a1c");
    }

    private boolean isBloodPressureObservation(ObservationEntity observation) {
        if (observation == null || observation.getCodeMaster() == null) {
            return false;
        }
        String loinc = safeUpper(observation.getCodeMaster().getLoincCode());
        String display = safeLower(observation.getCodeMaster().getCodeDisplay());
        return loinc.equals("8480-6") || loinc.equals("8462-4") || display.contains("blood pressure");
    }

    private boolean isCreatinineObservation(ObservationEntity observation) {
        if (observation == null || observation.getCodeMaster() == null) {
            return false;
        }
        String loinc = safeUpper(observation.getCodeMaster().getLoincCode());
        String display = safeLower(observation.getCodeMaster().getCodeDisplay());
        return loinc.equals("2160-0") || display.contains("creatinine");
    }

    private boolean isAbnormalA1c(ObservationEntity observation) {
        return hasHighInterpretation(observation)
                || (observation.getValueQuantity() != null && observation.getValueQuantity().compareTo(BigDecimal.valueOf(7.0)) > 0);
    }

    private boolean isAbnormalBloodPressure(ObservationEntity observation) {
        if (hasHighInterpretation(observation)) {
            return true;
        }
        String loinc = safeUpper(observation.getCodeMaster() != null ? observation.getCodeMaster().getLoincCode() : null);
        BigDecimal value = observation.getValueQuantity();
        if (value == null) {
            return false;
        }
        if (loinc.equals("8480-6")) {
            return value.compareTo(BigDecimal.valueOf(140)) >= 0;
        }
        if (loinc.equals("8462-4")) {
            return value.compareTo(BigDecimal.valueOf(90)) >= 0;
        }
        return value.compareTo(BigDecimal.valueOf(140)) >= 0;
    }

    private boolean isAbnormalCreatinine(ObservationEntity observation) {
        return hasHighInterpretation(observation)
                || (observation.getValueQuantity() != null && observation.getValueQuantity().compareTo(BigDecimal.valueOf(1.3)) > 0);
    }

    private boolean hasHighInterpretation(ObservationEntity observation) {
        String interpretation = safeUpper(observation != null ? observation.getInterpretationCode() : null);
        return interpretation.equals("H") || interpretation.equals("HH");
    }

    private boolean isA1cServiceRequest(ServiceRequestEntity request) {
        String codeValue = safeUpper(request.getCodeValue());
        String display = safeLower(request.getCodeDisplay());
        return codeValue.equals("4548-4") || display.contains("a1c") || display.contains("hemoglobin a1c");
    }

    private boolean isHypertensionServiceRequest(ServiceRequestEntity request) {
        String codeValue = safeUpper(request.getCodeValue());
        String display = safeLower(request.getCodeDisplay());
        String note = safeLower(request.getNote());
        return codeValue.equals("85354-9")
                || display.contains("blood pressure")
                || note.contains("blood pressure");
    }

    private boolean isCreatinineServiceRequest(ServiceRequestEntity request) {
        String codeValue = safeUpper(request.getCodeValue());
        String display = safeLower(request.getCodeDisplay());
        String note = safeLower(request.getNote());
        return codeValue.equals("2160-0")
                || display.contains("creatinine")
                || note.contains("creatinine");
    }

    private boolean isKidneyCancerServiceRequest(ServiceRequestEntity request) {
        String display = safeLower(request.getCodeDisplay());
        String note = safeLower(request.getNote());
        return display.contains("surveillance")
                || (display.contains("renal") && display.contains("imaging"))
                || note.contains("surveillance")
                || note.contains("kidney cancer");
    }

    private boolean isDiabetesFollowUpAppointment(AppointmentEntity appointment) {
        return appointmentTextContains(appointment, "diabet")
                || appointmentTextContains(appointment, "a1c")
                || appointmentTextContains(appointment, "endocr");
    }

    private boolean isHypertensionFollowUpAppointment(AppointmentEntity appointment) {
        return appointmentTextContains(appointment, "hypertension")
                || appointmentTextContains(appointment, "blood pressure")
                || appointmentTextContains(appointment, "bp follow-up");
    }

    private boolean isCkdFollowUpAppointment(AppointmentEntity appointment) {
        return appointmentTextContains(appointment, "ckd")
                || appointmentTextContains(appointment, "kidney disease")
                || appointmentTextContains(appointment, "renal function")
                || appointmentTextContains(appointment, "nephrology");
    }

    private boolean isKidneyCancerFollowUpAppointment(AppointmentEntity appointment) {
        return appointmentTextContains(appointment, "kidney cancer")
                || appointmentTextContains(appointment, "surveillance")
                || appointmentTextContains(appointment, "malignant neoplasm of kidney");
    }

    private boolean appointmentTextContains(AppointmentEntity appointment, String token) {
        return safeLower(appointment.getReasonDisplay()).contains(token)
                || safeLower(appointment.getDescription()).contains(token)
                || safeLower(appointment.getServiceTypeDisplay()).contains(token);
    }

    private boolean isActiveDiabetesMedication(MedicationRequestEntity request) {
        return isActiveMedication(request)
                && (safeUpper(request.getReasonCode()).startsWith("E10")
                || safeUpper(request.getReasonCode()).startsWith("E11")
                || safeLower(request.getReasonDisplay()).contains("diabet")
                || medicationTextContains(request, "metformin"));
    }

    private boolean isActiveHypertensionMedication(MedicationRequestEntity request) {
        return isActiveMedication(request)
                && (safeUpper(request.getReasonCode()).startsWith("I10")
                || safeLower(request.getReasonDisplay()).contains("hypertension")
                || medicationTextContains(request, "amlodipine")
                || medicationTextContains(request, "lisinopril"));
    }

    private boolean isActiveCkdMedication(MedicationRequestEntity request) {
        return isActiveMedication(request)
                && (safeUpper(request.getReasonCode()).startsWith("N18")
                || safeLower(request.getReasonDisplay()).contains("kidney")
                || medicationTextContains(request, "lisinopril"));
    }

    private boolean isActiveMedication(MedicationRequestEntity request) {
        return safeLower(request.getStatus()).equals("active");
    }

    private boolean medicationTextContains(MedicationRequestEntity request, String token) {
        String codeDisplay = request.getMedicationCode() != null ? safeLower(request.getMedicationCode().getCodeDisplay()) : "";
        String genericName = request.getMedicationCode() != null ? safeLower(request.getMedicationCode().getGenericName()) : "";
        return codeDisplay.contains(token) || genericName.contains(token) || safeLower(request.getDosageText()).contains(token);
    }

    private boolean isWithinPeriod(LocalDateTime dateTime, LocalDate periodStart, LocalDate periodEnd) {
        return dateTime != null
                && !dateTime.toLocalDate().isBefore(periodStart)
                && !dateTime.toLocalDate().isAfter(periodEnd);
    }

    private boolean matchesStatus(String gapStatus, String requestedStatus) {
        return requestedStatus.equals(ALL) || gapStatus.equals(requestedStatus);
    }

    private MeasureReport toMeasureReport(GapEvaluation evaluation, LocalDate periodStart, LocalDate periodEnd) {
        MeasureReport report = new MeasureReport();
        report.setId(IdType.newRandomUuid());
        report.setStatus(MeasureReport.MeasureReportStatus.COMPLETE);
        report.setType(MeasureReport.MeasureReportType.INDIVIDUAL);
        report.setMeasure(evaluation.gapType().measureUrl);
        report.setSubject(new Reference("Patient/" + evaluation.patient().getId())
                .setDisplay(patientDisplayName(evaluation.patient())));
        report.setDate(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
        report.setPeriod(new Period()
                .setStart(toDate(periodStart.atStartOfDay()))
                .setEnd(toDate(periodEnd.atTime(23, 59, 59))));
        report.addIdentifier()
                .setSystem("http://ehrassist.com/fhir/care-gaps")
                .setValue(evaluation.patient().getId() + "|" + evaluation.gapType().code + "|" + periodStart + "|" + periodEnd);
        report.addExtension(new Extension(GAP_STATUS_URL, new CodeType(evaluation.gapStatus())));
        report.addExtension(new Extension(GAP_DETAIL_URL, new StringType(buildSummary(evaluation))));

        if (evaluation.latestAbnormalObservationBeforePeriod() != null) {
            report.addExtension(new Extension("http://ehrassist.com/fhir/StructureDefinition/last-relevant-observation-date",
                    new DateTimeType(toDate(evaluation.latestAbnormalObservationBeforePeriod().getEffectiveDate()))));
            if (evaluation.latestAbnormalObservationBeforePeriod().getValueQuantity() != null) {
                report.addExtension(new Extension("http://ehrassist.com/fhir/StructureDefinition/last-relevant-observation-value",
                        new DecimalType(evaluation.latestAbnormalObservationBeforePeriod().getValueQuantity())));
            }
        }

        if (evaluation.latestQualifyingObservationInPeriod() != null) {
            report.addExtension(new Extension("http://ehrassist.com/fhir/StructureDefinition/latest-qualifying-observation-date",
                    new DateTimeType(toDate(evaluation.latestQualifyingObservationInPeriod().getEffectiveDate()))));
        }

        if (evaluation.latestRelevantOrder() != null && evaluation.latestRelevantOrder().getAuthoredOn() != null) {
            report.addExtension(new Extension("http://ehrassist.com/fhir/StructureDefinition/follow-up-order-date",
                    new DateTimeType(toDate(evaluation.latestRelevantOrder().getAuthoredOn()))));
        }

        if (evaluation.latestCompletedFollowUpAppointment() != null && evaluation.latestCompletedFollowUpAppointment().getStartTime() != null) {
            report.addExtension(new Extension("http://ehrassist.com/fhir/StructureDefinition/completed-follow-up-date",
                    new DateTimeType(toDate(evaluation.latestCompletedFollowUpAppointment().getStartTime()))));
        }

        if (evaluation.latestMissedFollowUpAppointment() != null && evaluation.latestMissedFollowUpAppointment().getStartTime() != null) {
            report.addExtension(new Extension("http://ehrassist.com/fhir/StructureDefinition/missed-follow-up-date",
                    new DateTimeType(toDate(evaluation.latestMissedFollowUpAppointment().getStartTime()))));
        }

        MeasureReport.MeasureReportGroupComponent group = report.addGroup();
        group.addPopulation()
                .setCode(populationCode("initial-population", "Initial Population"))
                .setCount(1);
        group.addPopulation()
                .setCode(populationCode("denominator", "Denominator"))
                .setCount(1);
        group.addPopulation()
                .setCode(populationCode("numerator", "Numerator"))
                .setCount(evaluation.gapStatus().equals(OPEN_GAP) ? 0 : 1);

        addEvaluatedResource(report, "Condition", evaluation.anchorCondition() != null ? evaluation.anchorCondition().getId() : null);
        addEvaluatedResource(report, "Observation", evaluation.latestAbnormalObservationBeforePeriod() != null ? evaluation.latestAbnormalObservationBeforePeriod().getId() : null);
        addEvaluatedResource(report, "Observation", evaluation.latestQualifyingObservationInPeriod() != null ? evaluation.latestQualifyingObservationInPeriod().getId() : null);
        addEvaluatedResource(report, "ServiceRequest", evaluation.latestRelevantOrder() != null ? evaluation.latestRelevantOrder().getId() : null);
        addEvaluatedResource(report, "Appointment", evaluation.latestCompletedFollowUpAppointment() != null ? evaluation.latestCompletedFollowUpAppointment().getId() : null);
        addEvaluatedResource(report, "Appointment", evaluation.latestMissedFollowUpAppointment() != null ? evaluation.latestMissedFollowUpAppointment().getId() : null);

        return report;
    }

    private org.hl7.fhir.r4.model.CodeableConcept populationCode(String code, String display) {
        return new org.hl7.fhir.r4.model.CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/measure-population")
                        .setCode(code)
                        .setDisplay(display));
    }

    private void addEvaluatedResource(MeasureReport report, String resourceType, UUID id) {
        if (id != null) {
            report.addEvaluatedResource(new Reference(resourceType + "/" + id));
        }
    }

    private String patientDisplayName(PatientEntity patient) {
        return patient.getNames().stream()
                .findFirst()
                .map(name -> name.getGivenFirst() + " " + name.getFamily())
                .orElse("Patient " + patient.getId());
    }

    private String buildSummary(GapEvaluation evaluation) {
        String patientName = patientDisplayName(evaluation.patient());
        StringBuilder summary = new StringBuilder(evaluation.gapType().label)
                .append(" is ")
                .append(evaluation.gapStatus())
                .append(" for ")
                .append(patientName)
                .append(".");

        if (evaluation.latestAbnormalObservationBeforePeriod() != null) {
            summary.append(" Last abnormal ")
                    .append(evaluation.gapType().observationLabel)
                    .append(" before the measurement period was ")
                    .append(evaluation.latestAbnormalObservationBeforePeriod().getValueQuantity())
                    .append(" ")
                    .append(safeString(evaluation.latestAbnormalObservationBeforePeriod().getValueUnit()))
                    .append(" on ")
                    .append(evaluation.latestAbnormalObservationBeforePeriod().getEffectiveDate().toLocalDate())
                    .append(".");
        }

        if (evaluation.latestRelevantOrder() != null && evaluation.latestRelevantOrder().getAuthoredOn() != null) {
            summary.append(" Follow-up order for ")
                    .append(evaluation.gapType().followUpAction)
                    .append(" was placed on ")
                    .append(evaluation.latestRelevantOrder().getAuthoredOn().toLocalDate())
                    .append(".");
        }

        if (evaluation.latestMissedFollowUpAppointment() != null && evaluation.latestMissedFollowUpAppointment().getStartTime() != null) {
            summary.append(" Follow-up appointment status was ")
                    .append(evaluation.latestMissedFollowUpAppointment().getStatus())
                    .append(" on ")
                    .append(evaluation.latestMissedFollowUpAppointment().getStartTime().toLocalDate())
                    .append(".");
        }

        if (evaluation.gapType() == GapType.KIDNEY_CANCER_SURVEILLANCE) {
            if (evaluation.latestCompletedFollowUpAppointment() != null && evaluation.latestCompletedFollowUpAppointment().getStartTime() != null) {
                summary.append(" A completed surveillance follow-up exists in the measurement period on ")
                        .append(evaluation.latestCompletedFollowUpAppointment().getStartTime().toLocalDate())
                        .append(".");
            } else {
                summary.append(" No completed surveillance follow-up exists in the measurement period.");
            }
        } else if (evaluation.latestQualifyingObservationInPeriod() != null && evaluation.latestQualifyingObservationInPeriod().getEffectiveDate() != null) {
            summary.append(" A qualifying ")
                    .append(evaluation.gapType().observationLabel)
                    .append(" observation exists in the measurement period on ")
                    .append(evaluation.latestQualifyingObservationInPeriod().getEffectiveDate().toLocalDate())
                    .append(".");
        } else {
            summary.append(" No qualifying ")
                    .append(evaluation.gapType().observationLabel)
                    .append(" observation exists in the measurement period.");
        }

        if (evaluation.hasRelevantMedication()) {
            summary.append(" Active relevant medication is on file.");
        }

        return summary.toString().replace("  ", " ").trim();
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String safeUpper(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private enum GapType {
        DIABETES_HBA1C("diabetes-hba1c-followup-gap", DIABETES_MEASURE_URL, "Diabetes HbA1c follow-up gap", "HbA1c", "repeat HbA1c"),
        HYPERTENSION_BP("hypertension-bp-followup-gap", HYPERTENSION_MEASURE_URL, "Hypertension blood pressure follow-up gap", "blood pressure", "blood pressure review"),
        CKD_CREATININE("ckd-creatinine-followup-gap", CKD_MEASURE_URL, "Chronic kidney disease creatinine follow-up gap", "creatinine", "repeat creatinine"),
        KIDNEY_CANCER_SURVEILLANCE("kidney-cancer-surveillance-gap", KIDNEY_CANCER_MEASURE_URL, "Kidney cancer surveillance gap", "renal function", "surveillance imaging");

        private final String code;
        private final String measureUrl;
        private final String label;
        private final String observationLabel;
        private final String followUpAction;

        GapType(String code, String measureUrl, String label, String observationLabel, String followUpAction) {
            this.code = code;
            this.measureUrl = measureUrl;
            this.label = label;
            this.observationLabel = observationLabel;
            this.followUpAction = followUpAction;
        }
    }

    private record GapEvaluation(PatientEntity patient,
                                 GapType gapType,
                                 String gapStatus,
                                 ConditionEntity anchorCondition,
                                 ObservationEntity latestAbnormalObservationBeforePeriod,
                                 ObservationEntity latestQualifyingObservationInPeriod,
                                 ServiceRequestEntity latestRelevantOrder,
                                 AppointmentEntity latestCompletedFollowUpAppointment,
                                 AppointmentEntity latestMissedFollowUpAppointment,
                                 boolean hasRelevantMedication) {
    }
}
