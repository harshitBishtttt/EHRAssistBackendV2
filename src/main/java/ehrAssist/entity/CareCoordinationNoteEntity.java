package ehrAssist.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "care_coordination_notes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareCoordinationNoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "coordinator_email", nullable = false, length = 255)
    private String coordinatorEmail;

    @Column(name = "coordinator_name", length = 255)
    private String coordinatorName;

    @Column(name = "coordinator_role", length = 255)
    private String coordinatorRole;

    @Column(name = "care_notes", columnDefinition = "NVARCHAR(MAX)")
    private String careNotes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "status")
    private String status;

    @Column(name = "is_active")
    private Boolean isActive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_note_id", nullable = false)
    private AIRecommendedActionsEntity aiRecommendedActionsEntity;

}
