package ehrAssist.mapper;

import ehrAssist.entity.CareCoordinationNoteEntity;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.stereotype.Component;

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
        if (entity.getCoordinatorName() != null && !entity.getCoordinatorName().isBlank()) {
            authorRef.setDisplay(entity.getCoordinatorName());
        }
        if (entity.getCoordinatorRole() != null && !entity.getCoordinatorRole().isBlank()) {
            authorRef.addExtension(new Extension(
                    "https://ehrassist.com/fhir/StructureDefinition/coordinator-role",
                    new StringType(entity.getCoordinatorRole())));
        }
        docRef.addAuthor(authorRef);

        if (entity.getCreatedAt() != null) {
            Date createdAt = Date.from(entity.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant());
            docRef.setDate(createdAt);
        }

        if (entity.getCareNotes() != null) {
            docRef.setDescription(entity.getCareNotes());
        }

        return docRef;
    }
}
