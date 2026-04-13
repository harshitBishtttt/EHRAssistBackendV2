package ehrAssist.mapper;

import ehrAssist.entity.DocumentReferenceEntity;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContentComponent;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContextComponent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class DocumentReferenceMapper {

    public DocumentReference toFhirResource(DocumentReferenceEntity entity) {
        DocumentReference docRef = new DocumentReference();

        docRef.setId(entity.getId().toString());

        Meta meta = new Meta();
        if (entity.getVersion() != null) {
            meta.setVersionId(entity.getVersion().toString());
        }
        if (entity.getUpdatedAt() != null) {
            meta.setLastUpdated(toDate(entity.getUpdatedAt()));
        }
        docRef.setMeta(meta);

        if (entity.getStatus() != null) {
            docRef.setStatus(DocumentReferenceStatus.fromCode(entity.getStatus()));
        }

        if (entity.getTypeCode() != null) {
            CodeableConcept type = new CodeableConcept();
            type.addCoding()
                    .setCode(entity.getTypeCode())
                    .setDisplay(entity.getTypeDisplay());
            docRef.setType(type);
        }

        if (entity.getPatient() != null) {
            docRef.setSubject(new Reference("Patient/" + entity.getPatient().getId()));
        }

        if (entity.getDescription() != null) {
            docRef.setDescription(entity.getDescription());
        }

        if (entity.getDate() != null) {
            docRef.setDate(toDate(entity.getDate()));
        }

        if (entity.getAuthor() != null) {
            String givenName = entity.getAuthor().getGivenName() == null ? "" : entity.getAuthor().getGivenName().trim();
            String familyName = entity.getAuthor().getFamilyName() == null ? "" : entity.getAuthor().getFamilyName().trim();
            String fullName = (givenName + " " + familyName).trim();
            Reference authorRef = new Reference("Practitioner/" + entity.getAuthor().getId());
            if (!fullName.isEmpty()) {
                authorRef.setDisplay(fullName);
            }
            if (entity.getAuthor().getSpecialtyDisplay() != null && !entity.getAuthor().getSpecialtyDisplay().isBlank()) {
                authorRef.addExtension("specialty", new StringType(entity.getAuthor().getSpecialtyDisplay()));
            }
            docRef.addAuthor(authorRef);
        }

        DocumentReferenceContentComponent content = docRef.addContent();
        Attachment attachment = new Attachment();
        if (entity.getContentType() != null) {
            attachment.setContentType(entity.getContentType());
        }
        if (entity.getContentData() != null) {
            attachment.setData(entity.getContentData());
        }
        if (entity.getContentTitle() != null) {
            attachment.setTitle(entity.getContentTitle());
        }
        content.setAttachment(attachment);

        if (entity.getEncounter() != null || entity.getPeriodStart() != null) {
            DocumentReferenceContextComponent context = new DocumentReferenceContextComponent();
            if (entity.getEncounter() != null) {
                context.addEncounter(new Reference("Encounter/" + entity.getEncounter().getId()));
            }
            if (entity.getPeriodStart() != null || entity.getPeriodEnd() != null) {
                Period period = new Period();
                if (entity.getPeriodStart() != null) {
                    period.setStart(toDate(entity.getPeriodStart()));
                }
                if (entity.getPeriodEnd() != null) {
                    period.setEnd(toDate(entity.getPeriodEnd()));
                }
                context.setPeriod(period);
            }
            docRef.setContext(context);
        }

        return docRef;
    }

    public DocumentReferenceEntity toEntity(DocumentReference fhir) {
        DocumentReferenceEntity entity = new DocumentReferenceEntity();
        entity.setContentType("text/plain");

        if (fhir.hasStatus()) {
            entity.setStatus(fhir.getStatus().toCode());
        }

        if (fhir.hasType() && fhir.getType().hasCoding()) {
            Coding coding = fhir.getType().getCodingFirstRep();
            entity.setTypeCode(coding.getCode());
            entity.setTypeDisplay(coding.getDisplay());
        }

        if (fhir.hasDescription()) {
            entity.setDescription(fhir.getDescription());
        }

        if (fhir.hasDate()) {
            entity.setDate(toLocalDateTime(fhir.getDate()));
        }

        if (fhir.hasContent() && !fhir.getContent().isEmpty()) {
            Attachment attachment = fhir.getContentFirstRep().getAttachment();
            if (attachment != null) {
                if (attachment.getContentType() != null && !attachment.getContentType().isBlank()) {
                    entity.setContentType(attachment.getContentType());
                }
                if (attachment.hasData()) {
                    entity.setContentData(attachment.getData());
                }
                entity.setContentTitle(attachment.getTitle());
            }
        }

        if (fhir.hasContext()) {
            DocumentReferenceContextComponent ctx = fhir.getContext();
            if (ctx.hasPeriod()) {
                if (ctx.getPeriod().hasStart()) {
                    entity.setPeriodStart(toLocalDateTime(ctx.getPeriod().getStart()));
                }
                if (ctx.getPeriod().hasEnd()) {
                    entity.setPeriodEnd(toLocalDateTime(ctx.getPeriod().getEnd()));
                }
            }
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
