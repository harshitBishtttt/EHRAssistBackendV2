package ehrAssist.mapper;

import ehrAssist.entity.AppointmentEntity;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Appointment.AppointmentStatus;
import org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class AppointmentMapper {

    public Appointment toFhirResource(AppointmentEntity entity) {
        Appointment appointment = new Appointment();

        appointment.setId(entity.getId().toString());

        Meta meta = new Meta();
        if (entity.getVersion() != null) {
            meta.setVersionId(entity.getVersion().toString());
        }
        if (entity.getUpdatedAt() != null) {
            meta.setLastUpdated(toDate(entity.getUpdatedAt()));
        }
        appointment.setMeta(meta);

        if (entity.getStatus() != null) {
            appointment.setStatus(AppointmentStatus.fromCode(entity.getStatus()));
        }

        if (entity.getStartTime() != null) {
            appointment.setStart(toDate(entity.getStartTime()));
        }

        if (entity.getEndTime() != null) {
            appointment.setEnd(toDate(entity.getEndTime()));
        }

        if (entity.getDescription() != null) {
            appointment.setDescription(entity.getDescription());
        }

        if (entity.getServiceTypeDisplay() != null) {
            CodeableConcept serviceType = new CodeableConcept();
            serviceType.setText(entity.getServiceTypeDisplay());
            appointment.addServiceType(serviceType);
        }

        if (entity.getReasonDisplay() != null) {
            CodeableConcept reason = new CodeableConcept();
            reason.setText(entity.getReasonDisplay());
            appointment.addReasonCode(reason);
        }

        if (entity.getPatient() != null) {
            AppointmentParticipantComponent patientParticipant = appointment.addParticipant();
            patientParticipant.setActor(new Reference("Patient/" + entity.getPatient().getId()));
            patientParticipant.setRequired(Appointment.ParticipantRequired.REQUIRED);
            patientParticipant.setStatus(Appointment.ParticipationStatus.ACCEPTED);
        }

        if (entity.getPractitioner() != null) {
            AppointmentParticipantComponent practitionerParticipant = appointment.addParticipant();
            practitionerParticipant.setActor(
                    new Reference("Practitioner/" + entity.getPractitioner().getId()));
            practitionerParticipant.setRequired(Appointment.ParticipantRequired.REQUIRED);
            practitionerParticipant.setStatus(Appointment.ParticipationStatus.ACCEPTED);
        }

        if (entity.getLocationName() != null) {
            appointment.addExtension("Location", new StringType(entity.getLocationName()));
        }

        return appointment;
    }

    public AppointmentEntity toEntity(Appointment fhir) {
        AppointmentEntity entity = new AppointmentEntity();

        if (fhir.hasStatus()) {
            entity.setStatus(fhir.getStatus().toCode());
        }

        if (fhir.hasStart()) {
            entity.setStartTime(toLocalDateTime(fhir.getStart()));
        }

        if (fhir.hasEnd()) {
            entity.setEndTime(toLocalDateTime(fhir.getEnd()));
        }

        if (fhir.hasDescription()) {
            entity.setDescription(fhir.getDescription());
        }

        if (fhir.hasServiceType() && !fhir.getServiceType().isEmpty()) {
            entity.setServiceTypeDisplay(fhir.getServiceTypeFirstRep().getText());
        }

        if (fhir.hasReasonCode() && !fhir.getReasonCode().isEmpty()) {
            entity.setReasonDisplay(fhir.getReasonCodeFirstRep().getText());
        }

        Extension locationExt = fhir.getExtensionByUrl("Location");
        if (locationExt != null) {
            entity.setLocationName(locationExt.getValue().primitiveValue());
        }

        return entity;
    }

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
