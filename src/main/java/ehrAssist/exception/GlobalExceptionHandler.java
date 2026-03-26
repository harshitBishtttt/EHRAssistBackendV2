package ehrAssist.exception;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final FhirContext fhirContext;

    public GlobalExceptionHandler(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleResourceNotFound(ResourceNotFoundException ex) {
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.NOTFOUND)
                .setDiagnostics(ex.getMessage());

        String body = fhirContext.newJsonParser().encodeResourceToString(outcome);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.parseMediaType("application/fhir+json"))
                .body(body);
    }

    @ExceptionHandler(FhirValidationException.class)
    public ResponseEntity<String> handleFhirValidation(FhirValidationException ex) {
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.INVALID)
                .setDiagnostics(ex.getMessage());

        String body = fhirContext.newJsonParser().encodeResourceToString(outcome);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.parseMediaType("application/fhir+json"))
                .body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneral(Exception ex) {
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.FATAL)
                .setCode(OperationOutcome.IssueType.EXCEPTION)
                .setDiagnostics(ex.getMessage());

        String body = fhirContext.newJsonParser().encodeResourceToString(outcome);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.parseMediaType("application/fhir+json"))
                .body(body);
    }
}
