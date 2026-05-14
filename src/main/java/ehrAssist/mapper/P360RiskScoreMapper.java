package ehrAssist.mapper;

import ehrAssist.entity.P360RiskScoreEntity;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Date;

@Component
public class P360RiskScoreMapper {

    public RiskAssessment toFhirResource(P360RiskScoreEntity entity) {
        RiskAssessment risk = new RiskAssessment();

        risk.setId(entity.getId().toString());
        risk.setStatus(RiskAssessment.RiskAssessmentStatus.FINAL);

        if (entity.getPatient() != null) {
            risk.setSubject(new Reference("Patient/" + entity.getPatient().getId()));
        }

        if (entity.getPractitioner() != null) {
            risk.setPerformer(new Reference("Practitioner/" + entity.getPractitioner().getId()));
        }

        if (entity.getCareManager() != null) {
            risk.addExtension()
                    .setUrl("http://ehrAssist/StructureDefinition/care-manager")
                    .setValue(new Reference("Practitioner/" + entity.getCareManager().getId()));
        }

        if (entity.getOrganization() != null) {
            risk.addExtension()
                    .setUrl("http://ehrAssist/StructureDefinition/organization")
                    .setValue(new Reference("Organization/" + entity.getOrganization().getId()));
        }

        if (entity.getRiskScore() != null) {
            RiskAssessment.RiskAssessmentPredictionComponent prediction = risk.addPrediction();
            prediction.setProbability(new DecimalType(entity.getRiskScore()));
        }

        if (entity.getCreatedDate() != null) {
            risk.setOccurrence(new DateTimeType(toDate(entity.getCreatedDate())));
        }

        return risk;
    }

    private Date toDate(java.time.LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
