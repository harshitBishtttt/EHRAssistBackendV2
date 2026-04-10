package ehrAssist.dto.response;

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
public class MarkReviewedResponse {
    private UUID reviewId;
    private UUID parentId;
    private Boolean isReviewed;
    private LocalDateTime createdDate;
}
