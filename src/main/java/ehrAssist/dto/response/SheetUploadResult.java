package ehrAssist.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SheetUploadResult {

    private String       sheetName;
    private String       targetTable;
    private String       status;
    private Integer      totalRows;
    private Integer      skippedEmptyRows;
    private Integer      insertedRows;
    private Integer      failedRows;
    private Long         tookMs;
    private List<String> ignoredColumns;
    private String       error;
}
