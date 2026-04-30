package ehrAssist.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    private String token;
    private String tokenType;
    private long expiresInMs;
    private UUID userId;
    private String email;
    private String role;
    private UUID refId;   // patientRefId or practitionerRefId based on role
}
