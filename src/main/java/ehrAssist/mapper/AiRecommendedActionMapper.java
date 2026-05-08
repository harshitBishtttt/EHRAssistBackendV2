package ehrAssist.mapper;

import ehrAssist.entity.AiRecommendedActionEntity;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.time.ZoneId;
import java.util.Date;

@Component
public class AiRecommendedActionMapper {

    private static final String CATEGORY_SYSTEM = "http://ehrassist.com/fhir/CodeSystem/communication-category";
    private static final String VERIFIED_BY_URL = "http://ehrassist.com/fhir/StructureDefinition/verified-by";
    private static final String VERIFIED_AT_URL = "http://ehrassist.com/fhir/StructureDefinition/verified-at";
    private static final String TITLE_URL = "http://ehrassist.com/fhir/StructureDefinition/action-title";
    private static final String PRIORITY_URL = "http://ehrassist.com/fhir/StructureDefinition/action-priority";
    private static final String URGENCY_URL = "http://ehrassist.com/fhir/StructureDefinition/action-urgency-note";

    public Communication toFhirResource(AiRecommendedActionEntity entity) {
        Communication comm = new Communication();

        comm.setId(entity.getId().toString());
        comm.setStatus(entity.getVerifiedAt() != null
                ? Communication.CommunicationStatus.COMPLETED
                : Communication.CommunicationStatus.PREPARATION);

        comm.addCategory(new CodeableConcept().addCoding(
                new Coding()
                        .setSystem(CATEGORY_SYSTEM)
                        .setCode("ai-recommended-action")
                        .setDisplay("AI Recommended Action")
        ));

        comm.setSubject(new Reference("Patient/" + entity.getPatientId()));
        comm.setSender(new Reference().setDisplay("EHRAssist AI Engine"));
        comm.addRecipient(new Reference("Patient/" + entity.getPatientId()));

        comm.addExtension(new Extension()
                .setUrl(TITLE_URL)
                .setValue(new StringType(entity.getTitle())));

        comm.addExtension(new Extension()
                .setUrl(PRIORITY_URL)
                .setValue(new StringType(entity.getPriority())));

        if (!ObjectUtils.isEmpty(entity.getUrgencyNote())) {
            comm.addExtension(new Extension()
                    .setUrl(URGENCY_URL)
                    .setValue(new StringType(entity.getUrgencyNote())));
        }

        Communication.CommunicationPayloadComponent payload = new Communication.CommunicationPayloadComponent();
        payload.setContent(new StringType(entity.getDescription()));
        comm.addPayload(payload);

        if (entity.getVerifiedAt() != null) {
            comm.setSent(Date.from(entity.getVerifiedAt().atZone(ZoneId.systemDefault()).toInstant()));

            if (entity.getPractitionerId() != null) {
                comm.addExtension(new Extension()
                        .setUrl(VERIFIED_BY_URL)
                        .setValue(new Reference("Practitioner/" + entity.getPractitionerId())));
            }

            comm.addExtension(new Extension()
                    .setUrl(VERIFIED_AT_URL)
                    .setValue(new DateTimeType(Date.from(
                            entity.getVerifiedAt().atZone(ZoneId.systemDefault()).toInstant()))));
        }

        return comm;
    }
}
