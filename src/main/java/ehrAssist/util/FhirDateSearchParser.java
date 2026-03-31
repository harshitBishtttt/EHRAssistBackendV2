package ehrAssist.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses FHIR R4 date search values (see <a href="https://hl7.org/fhir/R4/search.html#date">FHIR Search: date</a>):
 * optional prefix ({@code eq}, {@code ne}, {@code gt}, {@code lt}, {@code ge}, {@code le}, {@code sa}, {@code eb}, {@code ap})
 * followed by a {@code date} or {@code dateTime} string.
 * <p>
 * Examples: {@code ge2024-01-01}, {@code lt2024-12-31T10:00:00}, {@code eq2024-06-15} (prefix defaults to {@code eq}).
 */
public final class FhirDateSearchParser {

    private static final Pattern PREFIX_AND_REST = Pattern.compile(
            "^(?i)(eq|ne|gt|lt|ge|le|sa|eb|ap)?(.*)$");

    private FhirDateSearchParser() {
    }

    /**
     * @param raw URL-decoded search value, e.g. {@code ge2024-01-01}
     * @return parsed value, or null if invalid / blank
     */
    public static ParsedDateSearch parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        Matcher m = PREFIX_AND_REST.matcher(s);
        if (!m.matches()) {
            return null;
        }
        String prefixGroup = m.group(1);
        String prefix = prefixGroup != null ? prefixGroup.toLowerCase(Locale.ROOT) : "eq";
        String datePart = m.group(2).trim();
        if (datePart.isEmpty()) {
            return null;
        }

        LocalDateTime instant;
        boolean datePrecision;
        try {
            ParsedTemporal pt = parseTemporal(datePart);
            instant = pt.instant();
            datePrecision = pt.datePrecision();
        } catch (DateTimeParseException e) {
            return null;
        }

        return new ParsedDateSearch(prefix, instant, datePrecision);
    }

    private static ParsedTemporal parseTemporal(String datePart) {
        try {
            Instant instant = Instant.parse(datePart);
            return new ParsedTemporal(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()), false);
        } catch (DateTimeParseException ignored) {
        }
        try {
            OffsetDateTime odt = OffsetDateTime.parse(datePart, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return new ParsedTemporal(odt.toLocalDateTime(), false);
        } catch (DateTimeParseException ignored) {
        }
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(datePart, DateTimeFormatter.ISO_ZONED_DATE_TIME);
            return new ParsedTemporal(zdt.toLocalDateTime(), false);
        } catch (DateTimeParseException ignored) {
        }
        // Plain date-time (no zone)
        try {
            return new ParsedTemporal(LocalDateTime.parse(datePart, DateTimeFormatter.ISO_LOCAL_DATE_TIME), false);
        } catch (DateTimeParseException ignored) {
        }
        // Date only (calendar day precision)
        LocalDate d = LocalDate.parse(datePart, DateTimeFormatter.ISO_LOCAL_DATE);
        return new ParsedTemporal(d.atStartOfDay(), true);
    }

    private record ParsedTemporal(LocalDateTime instant, boolean datePrecision) {
    }

    /**
     * @param prefix       FHIR comparison prefix
     * @param instant      anchor instant (start of day when {@code datePrecision} is true)
     * @param datePrecision true if the value was date-only (YYYY-MM-DD)
     */
    public record ParsedDateSearch(String prefix, LocalDateTime instant, boolean datePrecision) {
    }
}
