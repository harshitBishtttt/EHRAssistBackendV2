package ehrAssist.service;

import org.hl7.fhir.r4.model.Bundle;

import java.time.LocalDate;

public interface CareGapService {
    Bundle evaluateDiabetesCareGaps(LocalDate periodStart,
                                    LocalDate periodEnd,
                                    String subject,
                                    String practitioner,
                                    String organization,
                                    String status);
}
