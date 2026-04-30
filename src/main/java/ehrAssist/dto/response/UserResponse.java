package ehrAssist.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ehrAssist.entity.UserAccountEntity;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private UUID id;
    private String email;
    private String role;
    private Boolean isActive;
    private UUID patientRefId;
    private UUID practitionerRefId;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    public static UserResponse from(UserAccountEntity entity) {
        return UserResponse.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .role(entity.getRole())
                .isActive(entity.getIsActive())
                .patientRefId(entity.getPatientRefId())
                .practitionerRefId(entity.getPractitionerRefId())
                .lastLoginAt(entity.getLastLoginAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
