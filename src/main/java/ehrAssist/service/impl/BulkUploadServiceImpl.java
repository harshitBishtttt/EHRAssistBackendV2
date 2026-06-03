package ehrAssist.service.impl;

import ehrAssist.dto.request.BulkUploadRequest;
import ehrAssist.dto.response.BulkUploadResponse;
import ehrAssist.dto.response.SheetUploadResult;
import ehrAssist.exception.BulkUploadException;
import ehrAssist.service.BulkUploadService;
import ehrAssist.util.GenericJdbcBulkInserter;
import ehrAssist.util.GenericJdbcBulkInserter.InsertOutcome;
import ehrAssist.util.SheetTableResolver;
import ehrAssist.util.WorkbookRowReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkUploadServiceImpl implements BulkUploadService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("xlsx", "xlsm");

    private final SheetTableResolver         resolver;
    private final WorkbookRowReader          rowReader;
    private final GenericJdbcBulkInserter    inserter;
    private final PlatformTransactionManager txManager;

    @Value("${bulk-upload.batch-size:500}")
    private int defaultBatchSize;

    @Value("${bulk-upload.max-rows-per-sheet:200000}")
    private int maxRowsPerSheet;

    @Override
    public BulkUploadResponse uploadWorkbook(MultipartFile file, BulkUploadRequest request) {

        validateRequest(file, request);

        boolean dryRun = Boolean.TRUE.equals(request.getDryRun());
        int batchSize  = (ObjectUtils.isEmpty(request.getBatchSize()) || request.getBatchSize() <= 0)
                ? defaultBatchSize
                : request.getBatchSize();

        long t0 = System.currentTimeMillis();

        try (InputStream in = file.getInputStream();
             Workbook wb   = WorkbookFactory.create(in)) {

            List<String> availableSheets = IntStream.range(0, wb.getNumberOfSheets())
                    .mapToObj(wb::getSheetName)
                    .filter(s -> !ObjectUtils.isEmpty(s))
                    .toList();

            List<String> allowedAvailableSheets = availableSheets.stream()
                    .filter(s -> resolver.isAllowed(resolver.resolveTable(s)))
                    .toList();

            List<String> requestedSheets = request.getSelectedSheets().stream()
                    .filter(s -> !ObjectUtils.isEmpty(s) && !s.trim().isEmpty())
                    .map(String::trim)
                    .distinct()
                    .toList();

            if (requestedSheets.isEmpty()) {
                throw new BulkUploadException(
                        "selectedSheets must contain at least one non-blank sheet name");
            }

            List<String> unknown = resolver.findUnknown(requestedSheets, availableSheets);

            TransactionTemplate tx = new TransactionTemplate(txManager);
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            List<SheetUploadResult> results = requestedSheets.stream()
                    .map(name -> processOneSheet(wb, name, availableSheets, batchSize, dryRun, tx))
                    .toList();

            int totalRead         = results.stream().mapToInt(r -> nz(r.getTotalRows())).sum();
            int totalInserted     = results.stream().mapToInt(r -> nz(r.getInsertedRows())).sum();
            int totalSkippedEmpty = results.stream().mapToInt(r -> nz(r.getSkippedEmptyRows())).sum();
            int totalFailed       = results.stream().mapToInt(r -> nz(r.getFailedRows())).sum();
            int totalProcessed    = (int) results.stream()
                    .filter(r -> "SUCCESS".equals(r.getStatus()) || "PARTIAL".equals(r.getStatus()))
                    .count();

            return BulkUploadResponse.builder()
                    .fileName(file.getOriginalFilename())
                    .availableSheets(allowedAvailableSheets)
                    .requestedSheets(requestedSheets)
                    .unknownRequestedSheets(unknown.isEmpty() ? null : unknown)
                    .results(results)
                    .totalSheetsProcessed(totalProcessed)
                    .totalRowsRead(totalRead)
                    .totalRowsInserted(totalInserted)
                    .totalRowsSkippedEmpty(totalSkippedEmpty)
                    .totalRowsFailed(totalFailed)
                    .dryRun(dryRun)
                    .totalTookMs(System.currentTimeMillis() - t0)
                    .executedAt(LocalDateTime.now())
                    .build();

        } catch (IOException e) {
            throw new BulkUploadException("Could not read workbook: " + e.getMessage(), e);
        }
    }

    private SheetUploadResult processOneSheet(Workbook wb,
                                              String requested,
                                              List<String> availableSheets,
                                              int batchSize,
                                              boolean dryRun,
                                              TransactionTemplate tx) {
        long t0 = System.currentTimeMillis();

        String actualName = availableSheets.stream()
                .filter(s -> !ObjectUtils.isEmpty(s))
                .filter(s -> s.trim().equalsIgnoreCase(requested.trim()))
                .findFirst()
                .orElse(null);

        if (ObjectUtils.isEmpty(actualName)) {
            return baseResult(requested, null, "SHEET_NOT_FOUND", t0)
                    .error("Sheet not present in workbook")
                    .build();
        }

        Sheet  sheet     = wb.getSheet(actualName);
        String tableName = resolver.resolveTable(actualName);

        if (!resolver.isAllowed(tableName)) {
            log.warn("Bulk upload rejected for sheet [{}] → table [{}] — not in allow-list.",
                    requested, tableName);
            return baseResult(requested, tableName, "INVALID_SHEET", t0)
                    .error("Sheet not found or incorrect sheet name; "
                         + "only registered business-entity sheets are accepted for bulk insert.")
                    .build();
        }

        List<String> headers = rowReader.readHeaders(sheet);
        if (CollectionUtils.isEmpty(headers)) {
            return baseResult(requested, tableName, "FAILED", t0)
                    .error("Sheet has no header row")
                    .build();
        }

        int totalPhysicalRows = rowReader.countDataRows(sheet);
        List<Object[]> rows   = rowReader.readDataRows(sheet, headers.size(), maxRowsPerSheet);
        int skippedEmpty      = totalPhysicalRows - rows.size();

        if (rows.isEmpty() || dryRun) {
            return baseResult(requested, tableName, "SUCCESS", t0)
                    .totalRows(totalPhysicalRows)
                    .skippedEmptyRows(skippedEmpty)
                    .build();
        }

        try {
            InsertOutcome outcome = tx.execute(s -> inserter.insert(tableName, headers, rows, batchSize));
            int inserted = ObjectUtils.isEmpty(outcome) ? 0 : outcome.inserted();
            int failed   = ObjectUtils.isEmpty(outcome) ? rows.size() : outcome.failed();
            List<String> ignored = ObjectUtils.isEmpty(outcome) ? List.of() : outcome.ignoredColumns();
            String error  = ObjectUtils.isEmpty(outcome) ? "Insert returned no outcome" : outcome.firstError();

            boolean hasError = !ObjectUtils.isEmpty(error);
            String status;
            if (failed == 0 && inserted == 0 && hasError) {
                status = "FAILED";
            } else if (failed == 0) {
                status = "SUCCESS";
            } else if (inserted == 0) {
                status = "FAILED";
            } else {
                status = "PARTIAL";
            }

            return baseResult(requested, tableName, status, t0)
                    .totalRows(totalPhysicalRows)
                    .insertedRows(inserted)
                    .skippedEmptyRows(skippedEmpty)
                    .failedRows(failed)
                    .ignoredColumns(ignored.isEmpty() ? null : ignored)
                    .error(error)
                    .build();

        } catch (Exception ex) {
            log.error("Sheet [{}] → table [{}] failed", requested, tableName, ex);
            return baseResult(requested, tableName, "FAILED", t0)
                    .totalRows(totalPhysicalRows)
                    .skippedEmptyRows(skippedEmpty)
                    .failedRows(rows.size())
                    .error(ex.getMessage())
                    .build();
        }
    }

    private SheetUploadResult.SheetUploadResultBuilder baseResult(String sheet,
                                                                  String table,
                                                                  String status,
                                                                  long t0) {
        return SheetUploadResult.builder()
                .sheetName(sheet)
                .targetTable(table)
                .status(status)
                .totalRows(0)
                .insertedRows(0)
                .skippedEmptyRows(0)
                .failedRows(0)
                .tookMs(System.currentTimeMillis() - t0);
    }

    private void validateRequest(MultipartFile file, BulkUploadRequest request) {
        if (ObjectUtils.isEmpty(file) || file.isEmpty()) {
            throw new BulkUploadException("File is required and must not be empty");
        }
        if (ObjectUtils.isEmpty(request)) {
            throw new BulkUploadException("Request JSON part is required (with at least selectedSheets)");
        }
        if (CollectionUtils.isEmpty(request.getSelectedSheets())) {
            throw new BulkUploadException("selectedSheets must contain at least one sheet name to process");
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BulkUploadException("Unsupported file type '" + ext + "'. Allowed: " + ALLOWED_EXTENSIONS);
        }
    }

    private String extensionOf(String fileName) {
        if (ObjectUtils.isEmpty(fileName)) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return (dot < 0 || dot == fileName.length() - 1) ? "" : fileName.substring(dot + 1).toLowerCase();
    }

    private int nz(Integer i) {
        return ObjectUtils.isEmpty(i) ? 0 : i;
    }
}
