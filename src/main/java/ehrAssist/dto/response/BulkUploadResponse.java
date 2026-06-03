package ehrAssist.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkUploadResponse {

    private String                  fileName;
    private List<String>            availableSheets;
    private List<String>            requestedSheets;
    private List<String>            unknownRequestedSheets;
    private List<SheetUploadResult> results;

    private Integer totalSheetsProcessed;
    private Integer totalRowsRead;
    private Integer totalRowsInserted;
    private Integer totalRowsSkippedEmpty;
    private Integer totalRowsFailed;

    private Boolean       dryRun;
    private Long          totalTookMs;
    private LocalDateTime executedAt;
}
