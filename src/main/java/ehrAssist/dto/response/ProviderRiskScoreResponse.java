package ehrAssist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderRiskScoreResponse {
    private UUID id;
    private BigDecimal riskScore;
    private UUID patientId;
    private UUID practitionerId;
    private UUID organizationId;
    private LocalDateTime createdDate;
}
