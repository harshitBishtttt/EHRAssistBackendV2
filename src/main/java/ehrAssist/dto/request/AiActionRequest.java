package ehrAssist.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiActionRequest {
    private UUID patientId;
    private String priority;
    private String action;
    private String description;
    private String aiRationale;
    private LocalDate dueDate;
}
