package ehrAssist.controller;

import ca.uhn.fhir.context.FhirContext;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Enumerations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/baseR4/metadata")
@RequiredArgsConstructor
public class MetadataController {

    private final FhirContext fhirContext;

    @GetMapping(produces = "application/fhir+json")
    public ResponseEntity<String> getMetadata() {
        CapabilityStatement cs = new CapabilityStatement();
        cs.setStatus(Enumerations.PublicationStatus.ACTIVE);
        cs.setDate(new Date());
        cs.setKind(CapabilityStatement.CapabilityStatementKind.INSTANCE);
        cs.setSoftware(new CapabilityStatement.CapabilityStatementSoftwareComponent()
                .setName("EHRAssistV2")
                .setVersion("2.0"));
        cs.setFhirVersion(Enumerations.FHIRVersion._4_0_1);
        cs.setFormat(List.of(new CodeType("application/fhir+json")));

        CapabilityStatement.CapabilityStatementRestComponent rest =
                new CapabilityStatement.CapabilityStatementRestComponent();
        rest.setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);

        List<String> resourceTypes = List.of(
                "Patient", "Practitioner", "Encounter", "Condition",
                "Observation", "Procedure", "MedicationRequest", "Appointment",
                "AllergyIntolerance", "DiagnosticReport", "ServiceRequest",
                "Immunization", "DocumentReference"
        );

        for (String type : resourceTypes) {
            CapabilityStatement.CapabilityStatementRestResourceComponent resource =
                    new CapabilityStatement.CapabilityStatementRestResourceComponent();
            resource.setType(type);
            resource.addInteraction().setCode(CapabilityStatement.TypeRestfulInteraction.READ);
            resource.addInteraction().setCode(CapabilityStatement.TypeRestfulInteraction.SEARCHTYPE);
            resource.addInteraction().setCode(CapabilityStatement.TypeRestfulInteraction.CREATE);
            resource.addInteraction().setCode(CapabilityStatement.TypeRestfulInteraction.UPDATE);
            resource.addInteraction().setCode(CapabilityStatement.TypeRestfulInteraction.DELETE);
            rest.addResource(resource);
        }

        cs.addRest(rest);

        String json = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(cs);
        return ResponseEntity.ok()
                .header("Content-Type", "application/fhir+json")
                .body(json);
    }
}
