package ehrAssist.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "patient_360_ai_recommended_actions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIRecommendedActionsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "status", nullable = false, length = 225)
    private String status;

    @Column(name = "priority", nullable = false, length = 225)
    private String priority;

    @Column(name = "action", nullable = false, length = 1000)
    private String action;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Column(name = "ai_rationale", nullable = false, length = 2000)
    private String aiRationale;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
