package ehrAssist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fhir_audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FhirAuditEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "recorded", nullable = false, updatable = false)
    private LocalDateTime recorded;

    /** FHIR AuditEvent.action: C | R | U | D | E */
    @Column(name = "action", nullable = false, length = 1)
    private String action;

    /** FHIR AuditEvent.outcome: 0 | 4 | 8 | 12 */
    @Column(name = "outcome", nullable = false, length = 2)
    private String outcome;

    @Column(name = "agent_user_id", nullable = false, length = 128)
    private String agentUserId;

    @Column(name = "agent_user_email", length = 255)
    private String agentUserEmail;

    @Column(name = "agent_ip", length = 45)
    private String agentIp;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "request_path", nullable = false, length = 500)
    private String requestPath;

    @Column(name = "request_query", length = 1000)
    private String requestQuery;

    @Column(name = "response_status", nullable = false)
    private Integer responseStatus;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "resource_id", length = 64)
    private String resourceId;

    @Column(name = "patient_id")
    private UUID patientId;
}
