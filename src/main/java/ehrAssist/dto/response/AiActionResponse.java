package ehrAssist.dto.response;

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
public class AiActionResponse {
    private UUID actionId;
    private UUID parentId;
    private String status;
    private String priority;
    private String action;
    private String description;
    private String aiRationale;

    private LocalDate dueDate;

}
