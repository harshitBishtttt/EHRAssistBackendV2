package ehrAssist.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCareCoordinationNoteRequest {
    private UUID patientId;
    private String coordinatorEmail;
    private String careNotes;
    private LocalDateTime createdAt;
}
