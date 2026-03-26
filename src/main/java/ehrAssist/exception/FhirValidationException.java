package ehrAssist.exception;

public class FhirValidationException extends RuntimeException {
    public FhirValidationException(String message) {
        super(message);
    }
}
