package ehrAssist.controller;

import ehrAssist.exception.ResourceNotFoundException;

import ehrAssist.mapper.*;
import ehrAssist.repository.*;
import ehrAssist.util.BundleBuilder;
import ehrAssist.util.FhirResponseHelper;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/baseR4/Patient/{patientId}")
@RequiredArgsConstructor
public class PatientEverythingController {

    private final PatientRepository patientRepository;
    private final FhirResponseHelper fhirResponseHelper;
    private final BundleBuilder bundleBuilder;

    private final ObservationRepository observationRepository;
    private final ObservationMapper observationMapper;

    private final ConditionRepository conditionRepository;
    private final ConditionMapper conditionMapper;

    private final EncounterRepository encounterRepository;
    private final EncounterMapper encounterMapper;

    private final MedicationRequestRepository medicationRequestRepository;
    private final MedicationRequestMapper medicationRequestMapper;

    private final ProcedureRepository procedureRepository;
    private final ProcedureMapper procedureMapper;

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;

    private final AllergyIntoleranceRepository allergyIntoleranceRepository;
    private final AllergyIntoleranceMapper allergyIntoleranceMapper;

    private final DiagnosticReportRepository diagnosticReportRepository;
    private final DiagnosticReportMapper diagnosticReportMapper;

    private final ImmunizationRepository immunizationRepository;
    private final ImmunizationMapper immunizationMapper;

    private final DocumentReferenceRepository documentReferenceRepository;
    private final DocumentReferenceMapper documentReferenceMapper;

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceRequestMapper serviceRequestMapper;

    private void validatePatientExists(UUID patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found: " + patientId);
        }
    }

    @GetMapping(value = "/Observation", produces = "application/fhir+json")
    public ResponseEntity<String> observations(@PathVariable UUID patientId) {
        validatePatientExists(patientId);
        List<Resource> resources = observationRepository.findByPatientId(patientId)
                .stream().map(observationMapper::toFhirResource).map(Resource.class::cast).toList();
        return fhirResponseHelper.toResponse(bundleBuilder.searchSet("Observation", resources, resources.size()));
    }

    @GetMapping(value = "/Condition", produces = "application/fhir+json")
    public ResponseEntity<String> conditions(@PathVariable UUID patientId) {
        validatePatientExists(patientId);
        List<Resource> resources = conditionRepository.findByPatientId(patientId)
                .stream().map(conditionMapper::toFhirResource).map(Resource.class::cast).toList();
        return fhirResponseHelper.toResponse(bundleBuilder.searchSet("Condition", resources, resources.size()));
    }

    @GetMapping(value = "/Encounter", produces = "application/fhir+json")
    public ResponseEntity<String> encounters(@PathVariable UUID patientId) {
        validatePatientExists(patientId);
        List<Resource> resources = encounterRepository.findByPatientId(patientId)
                .stream().map(encounterMapper::toFhirResource).map(Resource.class::cast).toList();
        return fhirResponseHelper.toResponse(bundleBuilder.searchSet("Encounter", resources, resources.size()));
    }

    @GetMapping(value = "/MedicationRequest", produces = "application/fhir+json")
    public ResponseEntity<String> medicationRequests(@PathVariable UUID patientId) {
        validatePatientExists(patientId);
        List<Resource> resources = medicationRequestRepository.findByPatientId(patientId)
                .stream().map(medicationRequestMapper::toFhirResource).map(Resource.class::cast).toList();
        return fhirResponseHelper.toResponse(bundleBuilder.searchSet("MedicationRequest", resources, resources.size()));
    }

    @GetMapping(value = "/Procedure", produces = "application/fhir+json")
    public ResponseEntity<String> procedures(@PathVariable UUID patientId) {
        validatePatientExists(patientId);
        List<Resource> resources = procedureRepository.findByPatientId(patientId)
                .stream().map(procedureMapper::toFhirResource).map(Resource.class::cast).toList();
        return fhirResponseHelper.toResponse(bundleBuilder.searchSet("Procedure", resources, resources.size()));
    }

    @GetMapping(value = "/Appointment", produces = "application/fhir+json")
    public ResponseEntity<String> appointments(@PathVariable UUID patientId) {
        validatePatientExists(patientId);
        List<Resource> resources = appointmentRepository.findByPatientId(patientId)
                .stream().map(appointmentMapper::toFhirResource).map(Resource.class::cast).toList();
        return fhirResponseHelper.toResponse(bundleBuilder.searchSet("Appointment", resources, resources.size()));
    }

    @GetMapping(value = "/AllergyIntolerance", produces = "application/fhir+json")
    public ResponseEntity<String> allergies(@PathVariable UUID patientId) {
        validatePatientExists(patientId);
        List<Resource> resources = allergyIntoleranceRepository.findByPatientId(patientId)
                .stream().map(allergyIntoleranceMapper::toFhirResource).map(Resource.class::cast).toList();
        return fhirResponseHelper.toResponse(bundleBuilder.searchSet("AllergyIntolerance", resources, resources.size()));
    }

    @GetMapping(value = "/DiagnosticReport", produces = "application/fhir+json")
    public ResponseEntity<String> diagnosticReports(@PathVariable UUID patientId) {
        validatePatientExists(patientId);
        List<Resource> resources = diagnosticReportRepository.findByPatientId(patientId)
                .stream().map(diagnosticReportMapper::toFhirResource).map(Resource.class::cast).toList();
        return fhirResponseHelper.toResponse(bundleBuilder.searchSet("DiagnosticReport", resources, resources.size()));
    }

    @GetMapping(value = "/Immunization", produces = "application/fhir+json")
    public ResponseEntity<String> immunizations(@PathVariable UUID patientId) {
        validatePatientExists(patientId);
        List<Resource> resources = immunizationRepository.findByPatientId(patientId)
                .stream().map(immunizationMapper::toFhirResource).map(Resource.class::cast).toList();
        return fhirResponseHelper.toResponse(bundleBuilder.searchSet("Immunization", resources, resources.size()));
    }

    @GetMapping(value = "/DocumentReference", produces = "application/fhir+json")
    public ResponseEntity<String> documentReferences(@PathVariable UUID patientId) {
        validatePatientExists(patientId);
        List<Resource> resources = documentReferenceRepository.findByPatientId(patientId)
                .stream().map(documentReferenceMapper::toFhirResource).map(Resource.class::cast).toList();
        return fhirResponseHelper.toResponse(bundleBuilder.searchSet("DocumentReference", resources, resources.size()));
    }

    @GetMapping(value = "/ServiceRequest", produces = "application/fhir+json")
    public ResponseEntity<String> serviceRequests(@PathVariable UUID patientId) {
        validatePatientExists(patientId);
        List<Resource> resources = serviceRequestRepository.findByPatientId(patientId)
                .stream().map(serviceRequestMapper::toFhirResource).map(Resource.class::cast).toList();
        return fhirResponseHelper.toResponse(bundleBuilder.searchSet("ServiceRequest", resources, resources.size()));
    }
}
