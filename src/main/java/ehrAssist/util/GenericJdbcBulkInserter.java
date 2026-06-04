package ehrAssist.util;

import ehrAssist.exception.BulkUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenericJdbcBulkInserter {

    private final JdbcTemplate jdbcTemplate;

    public record InsertOutcome(int inserted,
                                int failed,
                                List<String> ignoredColumns,
                                String firstError) { }

    private record TableMeta(Set<String> columns,
                             Set<String> identityColumns,
                             Map<String, String> columnTypes) { }

    private static final Set<String> BINARY_SQL_TYPES = Set.of("varbinary", "binary", "image");
    private static final Pattern BASE64_PATTERN       = Pattern.compile("^[A-Za-z0-9+/]+={0,2}$");

    public InsertOutcome insert(String tableName,
                                List<String> headers,
                                List<Object[]> rows,
                                int batchSize) {

        if (ObjectUtils.isEmpty(tableName) || CollectionUtils.isEmpty(headers)) {
            return new InsertOutcome(0, 0, List.of(), "Empty table name or headers");
        }
        if (CollectionUtils.isEmpty(rows)) {
            return new InsertOutcome(0, 0, List.of(), null);
        }

        TableMeta meta = loadTableMeta(tableName);
        if (meta.columns().isEmpty()) {
            return new InsertOutcome(0, 0, List.of(),
                    "Table " + tableName + " not found or has no columns");
        }

        List<Integer> usableIdx  = new ArrayList<>(headers.size());
        List<String>  usableCols = new ArrayList<>(headers.size());
        List<String>  ignored    = new ArrayList<>();
        IntStream.range(0, headers.size()).forEach(i -> {
            String h = headers.get(i);
            if (ObjectUtils.isEmpty(h)) {
                return;
            }
            if (meta.columns().contains(h)) {
                usableIdx.add(i);
                usableCols.add(h);
            } else {
                ignored.add(h);
            }
        });

        if (usableCols.isEmpty()) {
            return new InsertOutcome(0, 0, ignored,
                    "No sheet header matched any column in " + tableName);
        }

        boolean identityInsert = usableCols.stream().anyMatch(meta.identityColumns()::contains);

        List<String> usableDataTypes = usableCols.stream()
                .map(c -> meta.columnTypes().get(c))
                .toList();

        String columnList   = usableCols.stream().map(c -> "[" + c + "]").collect(Collectors.joining(", "));
        String placeholders = String.join(", ", java.util.Collections.nCopies(usableCols.size(), "?"));
        String insertSql    = "INSERT INTO [" + tableName + "] (" + columnList +
                              ") VALUES (" + placeholders + ")";

        log.debug("Bulk insert into {} — {} columns, {} rows, identityInsert={}",
                tableName, usableCols.size(), rows.size(), identityInsert);

        return jdbcTemplate.execute((Connection conn) ->
                doInsert(conn, tableName, insertSql, usableIdx, usableDataTypes,
                         rows, batchSize, identityInsert, ignored));
    }

    private InsertOutcome doInsert(Connection conn,
                                   String tableName,
                                   String insertSql,
                                   List<Integer> usableIdx,
                                   List<String> usableDataTypes,
                                   List<Object[]> rows,
                                   int batchSize,
                                   boolean identityInsert,
                                   List<String> ignored) {
        int inserted   = 0;
        int rowIndex   = 0;
        int paramCount = usableIdx.size();
        boolean identityOn = false;

        try {
            if (identityInsert) {
                setIdentityInsert(conn, tableName, true);
                identityOn = true;
            }
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (Object[] row : rows) {
                    rowIndex++;
                    bindRowOrAbort(ps, row, usableIdx, usableDataTypes, paramCount, tableName, rowIndex);
                    try {
                        ps.executeUpdate();
                        inserted++;
                    } catch (SQLException execEx) {
                        throw new BulkUploadException(
                                "Atomic abort: insert failed for [" + tableName + "] at sheet row "
                                        + rowIndex + " (" + inserted
                                        + " rows previously inserted in this transaction): "
                                        + execEx.getMessage(),
                                execEx);
                    }
                    ps.clearParameters();
                }
            }
        } catch (SQLException sqlEx) {
            throw new BulkUploadException(
                    "Atomic abort: bulk insert into [" + tableName + "] failed during prepare/setup: "
                            + sqlEx.getMessage(),
                    sqlEx);
        } finally {
            if (identityOn) {
                try {
                    setIdentityInsert(conn, tableName, false);
                } catch (SQLException e) {
                    log.warn("Failed to disable IDENTITY_INSERT on {}", tableName, e);
                }
            }
        }
        return new InsertOutcome(inserted, 0, ignored, null);
    }

    private void bindRowOrAbort(PreparedStatement ps,
                                Object[] row,
                                List<Integer> usableIdx,
                                List<String> usableDataTypes,
                                int paramCount,
                                String tableName,
                                int rowIndex) {
        for (int p = 0; p < paramCount; p++) {
            int sheetIdx = usableIdx.get(p);
            Object raw   = sheetIdx < row.length ? row[sheetIdx] : null;
            try {
                bindParam(ps, p + 1, raw, usableDataTypes.get(p));
            } catch (SQLException bindEx) {
                throw new BulkUploadException(
                        "Atomic abort: bind failed at sheet row " + rowIndex
                                + ", column position " + (p + 1) + " in [" + tableName + "]: "
                                + bindEx.getMessage(),
                        bindEx);
            }
        }
    }

    private static final Set<String> DATE_TYPES     = Set.of("date");
    private static final Set<String> DATETIME_TYPES = Set.of("datetime", "datetime2", "smalldatetime", "datetimeoffset");
    private static final Set<String> INT_TYPES      = Set.of("int", "integer", "smallint", "tinyint");
    private static final Set<String> BIGINT_TYPES   = Set.of("bigint");
    private static final Set<String> DECIMAL_TYPES  = Set.of("decimal", "numeric", "money", "smallmoney", "float", "real");
    private static final Set<String> BIT_TYPES      = Set.of("bit");
    private static final Set<String> STRING_TYPES   = Set.of("varchar", "nvarchar", "char", "nchar", "text", "ntext", "xml");

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern(
            "[yyyy-MM-dd HH:mm:ss][yyyy-MM-dd'T'HH:mm:ss][yyyy-MM-dd HH:mm][yyyy-MM-dd'T'HH:mm]");
    private static final DateTimeFormatter D_FMT  = DateTimeFormatter.ofPattern("[yyyy-MM-dd][MM/dd/yyyy][dd-MM-yyyy]");

    private void bindParam(PreparedStatement ps, int idx, Object raw, String dataType)
            throws SQLException {

        if (ObjectUtils.isEmpty(raw)) {
            ps.setObject(idx, null);
            return;
        }

        String dt = ObjectUtils.isEmpty(dataType) ? "" : dataType;

        if (BINARY_SQL_TYPES.contains(dt)) {
            byte[] bytes = toBinary(raw);
            if (bytes == null) {
                ps.setNull(idx, Types.VARBINARY);
            } else {
                ps.setBytes(idx, bytes);
            }
            return;
        }

        if ("uniqueidentifier".equals(dt)) {
            ps.setObject(idx, UUID.fromString(raw.toString().trim()));
            return;
        }

        if (BIT_TYPES.contains(dt)) {
            if (raw instanceof Boolean b) {
                ps.setBoolean(idx, b);
            } else {
                int v = ((Number) toNumber(raw)).intValue();
                ps.setBoolean(idx, v != 0);
            }
            return;
        }

        if (INT_TYPES.contains(dt)) {
            ps.setInt(idx, ((Number) toNumber(raw)).intValue());
            return;
        }

        if (BIGINT_TYPES.contains(dt)) {
            ps.setLong(idx, ((Number) toNumber(raw)).longValue());
            return;
        }

        if (DECIMAL_TYPES.contains(dt)) {
            if (raw instanceof BigDecimal bd) {
                ps.setBigDecimal(idx, bd);
            } else {
                ps.setBigDecimal(idx, new BigDecimal(raw.toString().trim()));
            }
            return;
        }

        if (DATE_TYPES.contains(dt)) {
            ps.setDate(idx, toSqlDate(raw));
            return;
        }

        if (DATETIME_TYPES.contains(dt)) {
            ps.setTimestamp(idx, toSqlTimestamp(raw));
            return;
        }

        if (STRING_TYPES.contains(dt)) {
            ps.setString(idx, raw.toString());
            return;
        }

        ps.setObject(idx, raw);
    }

    private Number toNumber(Object raw) {
        if (raw instanceof Number n) return n;
        String s = raw.toString().trim();
        if (s.isEmpty()) return 0;
        return new BigDecimal(s);
    }

    private Date toSqlDate(Object raw) {
        if (raw instanceof LocalDate ld) return Date.valueOf(ld);
        if (raw instanceof LocalDateTime ldt) return Date.valueOf(ldt.toLocalDate());
        if (raw instanceof java.util.Date d) return new Date(d.getTime());
        String s = raw.toString().trim();
        try {
            return Date.valueOf(LocalDate.parse(s, D_FMT));
        } catch (DateTimeParseException e) {
            return Date.valueOf(LocalDate.parse(s));
        }
    }

    private Timestamp toSqlTimestamp(Object raw) {
        if (raw instanceof LocalDateTime ldt) return Timestamp.valueOf(ldt);
        if (raw instanceof LocalDate ld) return Timestamp.valueOf(ld.atStartOfDay());
        if (raw instanceof java.util.Date d) return new Timestamp(d.getTime());
        String s = raw.toString().trim();
        try {
            return Timestamp.valueOf(LocalDateTime.parse(s, DT_FMT));
        } catch (DateTimeParseException e) {
            return Timestamp.valueOf(LocalDateTime.parse(s));
        }
    }

    private byte[] toBinary(Object raw) {
        if (ObjectUtils.isEmpty(raw)) {
            return null;
        }
        if (raw instanceof byte[] b) {
            return b;
        }
        if (raw instanceof String s) {
            return stringToBinary(s);
        }
        return raw.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] stringToBinary(String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > 2
                && (trimmed.regionMatches(true, 0, "0x", 0, 2))) {
            byte[] hex = tryHexDecode(trimmed.substring(2));
            if (hex != null) {
                return hex;
            }
        }
        if (trimmed.length() % 4 == 0 && BASE64_PATTERN.matcher(trimmed).matches()) {
            try {
                return Base64.getDecoder().decode(trimmed);
            } catch (IllegalArgumentException ignored) {
                // fall through to UTF-8
            }
        }
        return trimmed.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] tryHexDecode(String hex) {
        if (hex.isEmpty() || (hex.length() & 1) == 1) {
            return null;
        }
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2),     16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                return null;
            }
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    private void setIdentityInsert(Connection conn, String tableName, boolean on) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("SET IDENTITY_INSERT [" + tableName + "] " + (on ? "ON" : "OFF"));
        }
    }

    private TableMeta loadTableMeta(String tableName) {
        String sql = """
                SELECT  c.COLUMN_NAME,
                        c.DATA_TYPE,
                        COLUMNPROPERTY(OBJECT_ID(QUOTENAME(c.TABLE_SCHEMA) + '.' +
                                                 QUOTENAME(c.TABLE_NAME)),
                                       c.COLUMN_NAME, 'IsIdentity') AS IS_IDENTITY
                FROM    INFORMATION_SCHEMA.COLUMNS c
                WHERE   c.TABLE_NAME = ?
                ORDER BY c.ORDINAL_POSITION
                """;

        Set<String>         columns      = new HashSet<>();
        Set<String>         identityCols = new HashSet<>();
        Map<String, String> columnTypes  = new HashMap<>();

        jdbcTemplate.query(sql, rs -> {
            String raw = rs.getString("COLUMN_NAME");
            if (ObjectUtils.isEmpty(raw)) {
                return;
            }
            String name = raw.toLowerCase();
            columns.add(name);
            String dataType = rs.getString("DATA_TYPE");
            if (!ObjectUtils.isEmpty(dataType)) {
                columnTypes.put(name, dataType.toLowerCase());
            }
            if (rs.getInt("IS_IDENTITY") == 1) {
                identityCols.add(name);
            }
        }, tableName);

        return new TableMeta(columns, identityCols, columnTypes);
    }
}
