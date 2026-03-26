package ehrAssist.util;

import ca.uhn.fhir.context.FhirContext;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FhirResponseHelper {

    private static final MediaType FHIR_JSON = MediaType.parseMediaType("application/fhir+json");

    private final FhirContext fhirContext;

    public ResponseEntity<String> toResponse(Resource resource) {
        String json = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(resource);
        return ResponseEntity.ok().contentType(FHIR_JSON).body(json);
    }

    public ResponseEntity<String> toResponse(Bundle bundle) {
        String json = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
        return ResponseEntity.ok().contentType(FHIR_JSON).body(json);
    }
}
