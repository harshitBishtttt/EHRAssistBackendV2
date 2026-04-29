package ehrAssist.dto.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface RiskFeedProjection {

    UUID       getPatientId();
    String     getFullName();
    String     getGender();
    String     getPhone();
    String     getEmail();
    String     getLoincCode();
    String     getCodeSystem();
    String     getDisplay();
    BigDecimal getValueQuantity();
    String     getUnit();
    String     getInterpretationCode();
    LocalDateTime getEffectiveDate();
    BigDecimal getReferenceRangeLow();
    BigDecimal getReferenceRangeHigh();
}
