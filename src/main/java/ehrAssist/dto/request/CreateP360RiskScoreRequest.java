package ehrAssist.dto.request;

import jakarta.validation.constraints.NotNull;
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
public class CreateP360RiskScoreRequest {

    @NotNull(message = "riskScore is required")
    private BigDecimal riskScore;

    @NotNull(message = "patientId is required")
    private UUID patientId;

    @NotNull(message = "organizationId is required")
    private UUID organizationId;

    private UUID practitionerId;

    private UUID careManagerId;
}
