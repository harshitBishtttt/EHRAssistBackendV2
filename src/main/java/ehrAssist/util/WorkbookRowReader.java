package ehrAssist.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Component
public class WorkbookRowReader {

    public List<String> readHeaders(Sheet sheet) {
        if (ObjectUtils.isEmpty(sheet)) {
            return List.of();
        }
        Row header = sheet.getRow(sheet.getFirstRowNum());
        if (ObjectUtils.isEmpty(header)) {
            return List.of();
        }
        return IntStream.range(0, header.getLastCellNum())
                .mapToObj(i -> header.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL))
                .map(this::headerText)
                .toList();
    }

    public int countDataRows(Sheet sheet) {
        if (ObjectUtils.isEmpty(sheet)) {
            return 0;
        }
        return Math.max(0, sheet.getLastRowNum() - sheet.getFirstRowNum());
    }

    public List<Object[]> readDataRows(Sheet sheet, int columnCount, int maxRows) {
        if (ObjectUtils.isEmpty(sheet)) {
            return List.of();
        }
        int first = sheet.getFirstRowNum() + 1;
        int last  = sheet.getLastRowNum();
        if (first > last) {
            return List.of();
        }
        return IntStream.rangeClosed(first, last)
                .mapToObj(r -> readNonEmptyRow(sheet.getRow(r), columnCount))
                .filter(Objects::nonNull)
                .limit(maxRows)
                .toList();
    }

    public Object[] readNonEmptyRow(Row row, int columnCount) {
        if (ObjectUtils.isEmpty(row)) {
            return null;
        }
        Object[] values = IntStream.range(0, columnCount)
                .mapToObj(i -> coerce(row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)))
                .toArray();
        boolean hasValue = Arrays.stream(values).anyMatch(v -> !ObjectUtils.isEmpty(v));
        return hasValue ? values : null;
    }

    public Object coerce(Cell cell) {
        if (ObjectUtils.isEmpty(cell) || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING  -> coerceString(cell.getStringCellValue());
            case BOOLEAN -> cell.getBooleanCellValue();
            case NUMERIC -> coerceNumeric(cell);
            case FORMULA -> coerceFormula(cell);
            default      -> null;
        };
    }

    private String headerText(Cell cell) {
        if (ObjectUtils.isEmpty(cell)) {
            return null;
        }
        String raw = (cell.getCellType() == CellType.STRING)
                ? cell.getStringCellValue()
                : cell.toString();
        return ObjectUtils.isEmpty(raw) ? null : raw.trim().toLowerCase();
    }

    private Object coerceString(String raw) {
        if (ObjectUtils.isEmpty(raw)) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Object coerceNumeric(Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            LocalDateTime ldt = cell.getLocalDateTimeCellValue();
            if (ObjectUtils.isEmpty(ldt)) {
                return null;
            }
            return (ldt.getHour() == 0 && ldt.getMinute() == 0 && ldt.getSecond() == 0)
                    ? LocalDate.of(ldt.getYear(), ldt.getMonth(), ldt.getDayOfMonth())
                    : ldt;
        }
        double d = cell.getNumericCellValue();
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            if (d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE) {
                return (int) d;
            }
            if (d >= Long.MIN_VALUE && d <= Long.MAX_VALUE) {
                return (long) d;
            }
        }
        return BigDecimal.valueOf(d);
    }

    private Object coerceFormula(Cell cell) {
        return switch (cell.getCachedFormulaResultType()) {
            case STRING  -> coerceString(cell.getStringCellValue());
            case BOOLEAN -> cell.getBooleanCellValue();
            case NUMERIC -> coerceNumeric(cell);
            default      -> null;
        };
    }
}
