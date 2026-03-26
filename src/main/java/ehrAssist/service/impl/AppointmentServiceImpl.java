package ehrAssist.service.impl;

import ehrAssist.entity.AppointmentEntity;
import ehrAssist.exception.ResourceNotFoundException;
import ehrAssist.mapper.AppointmentMapper;
import ehrAssist.repository.AppointmentRepository;
import ehrAssist.repository.PatientRepository;
import ehrAssist.repository.PractitionerRepository;
import ehrAssist.service.AppointmentService;
import ehrAssist.util.BundleBuilder;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Bundle;
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
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final PractitionerRepository practitionerRepository;
    private final AppointmentMapper appointmentMapper;
    private final BundleBuilder bundleBuilder;

    @Override
    @Transactional(readOnly = true)
    public Appointment getById(UUID id) {
        AppointmentEntity entity = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));
        return appointmentMapper.toFhirResource(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Bundle search(UUID id, UUID patientId, String status, Pageable pageable) {
        if (id == null && patientId == null && status == null) {
            return bundleBuilder.searchSetWithPagination("Appointment", List.of(), 0L, 0, pageable.getPageSize(), "");
        }

        Specification<AppointmentEntity> spec = Specification.where(null);

        if (id != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("id"), id));
        }
        if (patientId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("patient").get("id"), patientId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        Page<AppointmentEntity> pageResult = appointmentRepository.findAll(spec, pageable);

        List<Resource> fhirResources = pageResult.getContent().stream()
                .map(appointmentMapper::toFhirResource)
                .map(Resource.class::cast)
                .toList();

        StringBuilder queryParams = new StringBuilder();
        if (id != null) queryParams.append("_id=").append(id).append("&");
        if (patientId != null) queryParams.append("patient=").append(patientId).append("&");
        if (status != null) queryParams.append("status=").append(status).append("&");
        String query = queryParams.length() > 0 ? queryParams.substring(0, queryParams.length() - 1) : "";

        return bundleBuilder.searchSetWithPagination("Appointment", fhirResources, pageResult.getTotalElements(), 
                pageable.getPageNumber(), pageable.getPageSize(), query);
    }

    @Override
    public Appointment create(Appointment resource) {
        AppointmentEntity entity = appointmentMapper.toEntity(resource);

        if (resource.hasParticipant()) {
            for (var participant : resource.getParticipant()) {
                if (participant.hasActor()) {
                    String ref = participant.getActor().getReference();
                    if (ref != null && ref.startsWith("Patient/")) {
                        UUID patientId = UUID.fromString(ref.split("/")[1]);
                        entity.setPatient(patientRepository.findById(patientId)
                                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId)));
                    } else if (ref != null && ref.startsWith("Practitioner/")) {
                        UUID practId = UUID.fromString(ref.split("/")[1]);
                        entity.setPractitioner(practitionerRepository.findById(practId)
                                .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found: " + practId)));
                    }
                }
            }
        }

        AppointmentEntity saved = appointmentRepository.save(entity);
        return appointmentMapper.toFhirResource(saved);
    }

    @Override
    public Appointment update(UUID id, Appointment resource) {
        AppointmentEntity existing = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));

        AppointmentEntity updated = appointmentMapper.toEntity(resource);

        existing.setStatus(updated.getStatus());
        existing.setServiceTypeCode(updated.getServiceTypeCode());
        existing.setServiceTypeDisplay(updated.getServiceTypeDisplay());
        existing.setReasonCode(updated.getReasonCode());
        existing.setReasonDisplay(updated.getReasonDisplay());
        existing.setDescription(updated.getDescription());
        existing.setLocationName(updated.getLocationName());
        existing.setStartTime(updated.getStartTime());
        existing.setEndTime(updated.getEndTime());

        if (resource.hasParticipant()) {
            for (var participant : resource.getParticipant()) {
                if (participant.hasActor()) {
                    String ref = participant.getActor().getReference();
                    if (ref != null && ref.startsWith("Patient/")) {
                        UUID patientId = UUID.fromString(ref.split("/")[1]);
                        existing.setPatient(patientRepository.findById(patientId)
                                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId)));
                    } else if (ref != null && ref.startsWith("Practitioner/")) {
                        UUID practId = UUID.fromString(ref.split("/")[1]);
                        existing.setPractitioner(practitionerRepository.findById(practId)
                                .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found: " + practId)));
                    }
                }
            }
        }

        AppointmentEntity saved = appointmentRepository.save(existing);
        return appointmentMapper.toFhirResource(saved);
    }

    @Override
    public void delete(UUID id) {
        AppointmentEntity entity = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));
        appointmentRepository.delete(entity);
    }
}
