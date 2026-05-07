package ehrAssist.mapper;

import ehrAssist.entity.AiRecommendationEntity;
import ehrAssist.entity.AiRecommendationPayloadEntity;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;

@Component
public class AiRecommendationMapper {

    private static final String CATEGORY_SYSTEM = "http://ehrassist.com/fhir/CodeSystem/communication-category";
    private static final String VERIFIED_BY_URL = "http://ehrassist.com/fhir/StructureDefinition/verified-by";
    private static final String VERIFIED_AT_URL = "http://ehrassist.com/fhir/StructureDefinition/verified-at";

    public Communication toFhirResource(AiRecommendationEntity entity) {
        Communication comm = new Communication();

        comm.setId(entity.getId().toString());
        comm.setStatus(entity.getVerifiedAt() != null
                ? Communication.CommunicationStatus.COMPLETED
                : Communication.CommunicationStatus.PREPARATION);

        comm.addCategory(new CodeableConcept().addCoding(
                new Coding()
                        .setSystem(CATEGORY_SYSTEM)
                        .setCode(entity.getCategory())
                        .setDisplay("AI Recommended Instructions")
        ));

        comm.setSubject(new Reference("Patient/" + entity.getPatientId()));
        comm.setSender(new Reference().setDisplay("EHRAssist AI Engine"));
        comm.addRecipient(new Reference("Patient/" + entity.getPatientId()));

        if (entity.getVerifiedAt() != null) {
            comm.setSent(Date.from(entity.getVerifiedAt().atZone(ZoneId.systemDefault()).toInstant()));

            if (entity.getVerifiedBy() != null) {
                comm.addExtension(new Extension()
                        .setUrl(VERIFIED_BY_URL)
                        .setValue(new Reference("Practitioner/" + entity.getVerifiedBy())));
            }

            comm.addExtension(new Extension()
                    .setUrl(VERIFIED_AT_URL)
                    .setValue(new DateTimeType(Date.from(
                            entity.getVerifiedAt().atZone(ZoneId.systemDefault()).toInstant()))));
        }

        entity.getPayloads().stream()
                .sorted(Comparator.comparingInt(AiRecommendationPayloadEntity::getSequence))
                .forEach(payload -> {
                    Communication.CommunicationPayloadComponent payloadComponent =
                            new Communication.CommunicationPayloadComponent();
                    payloadComponent.setContent(new StringType(payload.getContentString()));
                    comm.addPayload(payloadComponent);
                });

        return comm;
    }
}
