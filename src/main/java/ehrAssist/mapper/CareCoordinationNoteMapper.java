package ehrAssist.mapper;

import ehrAssist.entity.AIRecommendedActionsEntity;
import ehrAssist.entity.CareCoordinationNoteEntity;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.time.ZoneId;
import java.util.Date;

@Component
public class CareCoordinationNoteMapper {

    public DocumentReference toFhirResource(CareCoordinationNoteEntity entity) {
        DocumentReference docRef = new DocumentReference();
        docRef.setId(entity.getId().toString());

        docRef.setSubject(new Reference("Patient/" + entity.getPatientId()));

        Reference authorRef = new Reference();
        authorRef.setIdentifier(new Identifier()
                .setSystem("mailto")
                .setValue(entity.getCoordinatorEmail()));

        if (!ObjectUtils.isEmpty(entity.getCoordinatorName())) {
            authorRef.setDisplay(entity.getCoordinatorName());
        }
        if (!ObjectUtils.isEmpty(entity.getCoordinatorRole())) {
            authorRef.addExtension(new Extension(
                    "coordinator-role",
                    new StringType(entity.getCoordinatorRole())));
        }
        docRef.addAuthor(authorRef);

        if (!ObjectUtils.isEmpty(entity.getCreatedAt())) {
            Date createdAt = Date.from(entity.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant());
            docRef.setDate(createdAt);
        }

        if (!ObjectUtils.isEmpty(entity.getCareNotes())) {
            docRef.setDescription(entity.getCareNotes());
        }

        if (!ObjectUtils.isEmpty(entity.getStatus())) {
            docRef.addExtension(new Extension("status", new StringType(entity.getStatus())));
        }

        AIRecommendedActionsEntity recommendedAction = entity.getAiRecommendedActionsEntity();
        if (!ObjectUtils.isEmpty(recommendedAction)) {
            if (!ObjectUtils.isEmpty(recommendedAction.getId())) {
                docRef.addExtension(new Extension("actionId", new StringType(recommendedAction.getId().toString())));
            }
            if (!ObjectUtils.isEmpty(recommendedAction.getAction())) {
                docRef.addExtension(new Extension("recommended-action", new StringType(recommendedAction.getAction())));
            }
        }

        return docRef;
    }
}
