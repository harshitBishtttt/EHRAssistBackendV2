package ehrAssist.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "CARE_MANAGER|PATIENT|PROVIDER", message = "Role must be CARE_MANAGER, PATIENT, or PROVIDER")
    private String role;

    private UUID patientRefId;       // required if role = PATIENT
    private UUID practitionerRefId;  // required if role = PROVIDER
}
