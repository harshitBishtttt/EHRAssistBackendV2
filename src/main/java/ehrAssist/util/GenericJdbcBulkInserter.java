package ehrAssist.util;

import ehrAssist.exception.BulkUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
                int batched = 0;
                for (Object[] row : rows) {
                    rowIndex++;
                    bindRowOrAbort(ps, row, usableIdx, usableDataTypes, paramCount, tableName, rowIndex);
                    ps.addBatch();
                    if (++batched >= batchSize) {
                        inserted += flushBatchOrAbort(ps, tableName, inserted, rowIndex);
                        batched = 0;
                    }
                }
                if (batched > 0) {
                    inserted += flushBatchOrAbort(ps, tableName, inserted, rowIndex);
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

    private int flushBatchOrAbort(PreparedStatement ps,
                                  String tableName,
                                  int rowsBufferedBefore,
                                  int currentRow) {
        try {
            return sumCounts(ps.executeBatch());
        } catch (SQLException batchEx) {
            throw new BulkUploadException(
                    "Atomic abort: batch flush failed for [" + tableName + "] near sheet row "
                            + currentRow + " (" + rowsBufferedBefore
                            + " rows previously buffered in this transaction): "
                            + batchEx.getMessage(),
                    batchEx);
        }
    }

    private void bindParam(PreparedStatement ps, int idx, Object raw, String dataType)
            throws SQLException {
        boolean isBinary = !ObjectUtils.isEmpty(dataType) && BINARY_SQL_TYPES.contains(dataType);

        if (isBinary) {
            byte[] bytes = toBinary(raw);
            if (bytes == null) {
                ps.setNull(idx, Types.VARBINARY);
            } else {
                ps.setBytes(idx, bytes);
            }
            return;
        }

        if (ObjectUtils.isEmpty(raw)) {
            ps.setObject(idx, null);
        } else {
            ps.setObject(idx, raw);
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

    private int sumCounts(int[] counts) {
        if (ObjectUtils.isEmpty(counts)) {
            return 0;
        }
        return Arrays.stream(counts)
                .map(c -> c == Statement.SUCCESS_NO_INFO ? 1 : Math.max(0, c))
                .sum();
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
