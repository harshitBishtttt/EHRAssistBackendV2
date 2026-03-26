package ehrAssist.controller;

import ehrAssist.exception.ResourceNotFoundException;
import ehrAssist.mapper.*;
import ehrAssist.repository.*;
import ehrAssist.util.BundleBuilder;
import ehrAssist.util.FhirResponseHelper;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/baseR4/Patient/{patientId}/clinical-data")
@RequiredArgsConstructor
public class PatientClinicalDataController {

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

    @GetMapping(produces = "application/fhir+json")
    public ResponseEntity<String> getAllClinicalData(
            @PathVariable UUID patientId,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found: " + patientId);
        }

        List<Resource> allResources = new ArrayList<>();

        observationRepository.findByPatientId(patientId).forEach(e -> 
            allResources.add(observationMapper.toFhirResource(e)));
        
        conditionRepository.findByPatientId(patientId).forEach(e -> 
            allResources.add(conditionMapper.toFhirResource(e)));
        
        encounterRepository.findByPatientId(patientId).forEach(e -> 
            allResources.add(encounterMapper.toFhirResource(e)));
        
        medicationRequestRepository.findByPatientId(patientId).forEach(e -> 
            allResources.add(medicationRequestMapper.toFhirResource(e)));
        
        procedureRepository.findByPatientId(patientId).forEach(e -> 
            allResources.add(procedureMapper.toFhirResource(e)));
        
        appointmentRepository.findByPatientId(patientId).forEach(e -> 
            allResources.add(appointmentMapper.toFhirResource(e)));
        
        allergyIntoleranceRepository.findByPatientId(patientId).forEach(e -> 
            allResources.add(allergyIntoleranceMapper.toFhirResource(e)));
        
        diagnosticReportRepository.findByPatientId(patientId).forEach(e -> 
            allResources.add(diagnosticReportMapper.toFhirResource(e)));
        
        immunizationRepository.findByPatientId(patientId).forEach(e -> 
            allResources.add(immunizationMapper.toFhirResource(e)));
        
        documentReferenceRepository.findByPatientId(patientId).forEach(e -> 
            allResources.add(documentReferenceMapper.toFhirResource(e)));
        
        serviceRequestRepository.findByPatientId(patientId).forEach(e -> 
            allResources.add(serviceRequestMapper.toFhirResource(e)));

        int total = allResources.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        
        List<Resource> paginatedResources = start < total ? 
            allResources.subList(start, end) : new ArrayList<>();

        Bundle bundle = bundleBuilder.searchSetWithPagination(
            "Patient/" + patientId + "/clinical-data", 
            paginatedResources, 
            total, 
            pageable.getPageNumber(), 
            pageable.getPageSize(), 
            ""
        );

        return fhirResponseHelper.toResponse(bundle);
    }
}
