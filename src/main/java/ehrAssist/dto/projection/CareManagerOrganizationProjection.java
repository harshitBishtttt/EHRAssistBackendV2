package ehrAssist.dto.projection;

import java.time.LocalDateTime;
import java.util.UUID;

public interface CareManagerOrganizationProjection {
    UUID getCareManagerId();

    UUID getId();

    Boolean getActive();

    String getName();

    String getTypeCode();

    String getTypeDisplay();

    String getPhone();

    String getAddressCity();

    String getAddressState();

    Integer getVersion();

    LocalDateTime getCreatedAt();
}
