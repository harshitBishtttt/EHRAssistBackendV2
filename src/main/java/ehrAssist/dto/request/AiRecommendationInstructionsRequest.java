package ehrAssist.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendationInstructionsRequest {

    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    private UUID practitionerId;

    @NotEmpty(message = "At least one recommendation payload is required")
    private List<String> payloads;
}
