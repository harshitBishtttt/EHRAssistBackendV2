package ehrAssist.util;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses FHIR Observation search parameter {@code value-quantity}, e.g. {@code gt1|mEq/L}
 * (URL-decoded: prefix + number, pipe, UCUM unit).
 */
public final class FhirQuantitySearchParser {

    private static final Pattern PREFIX_AND_NUMBER = Pattern.compile(
            "^(?i)(eq|ne|gt|lt|ge|le|sa|eb)?([0-9.+-eE]+)$");

    private FhirQuantitySearchParser() {
    }

    /**
     * @param raw e.g. {@code gt1|mEq/L} or {@code eq5} (Spring decodes query string)
     * @return parsed parts, or null if invalid / blank
     */
    public static ParsedValueQuantity parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        String unitPart = null;
        int pipe = s.indexOf('|');
        if (pipe >= 0) {
            unitPart = s.substring(pipe + 1).trim();
            s = s.substring(0, pipe).trim();
        }
        Matcher m = PREFIX_AND_NUMBER.matcher(s);
        if (!m.matches()) {
            return null;
        }
        String prefixGroup = m.group(1);
        String prefix = prefixGroup != null ? prefixGroup.toLowerCase(Locale.ROOT) : "eq";
        BigDecimal number;
        try {
            number = new BigDecimal(m.group(2));
        } catch (NumberFormatException e) {
            return null;
        }
        return new ParsedValueQuantity(prefix, number, unitPart);
    }

    public record ParsedValueQuantity(String prefix, BigDecimal number, String unit) {

        public boolean hasUnit() {
            return unit != null && !unit.isBlank();
        }
    }
}
