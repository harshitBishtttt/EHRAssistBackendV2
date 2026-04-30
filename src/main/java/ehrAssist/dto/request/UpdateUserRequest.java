package ehrAssist.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Pattern(regexp = "CARE_MANAGER|PATIENT|PROVIDER", message = "Role must be CARE_MANAGER, PATIENT, or PROVIDER")
    private String role;

    private Boolean isActive;
}
