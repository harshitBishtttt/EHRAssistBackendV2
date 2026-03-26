package ehrAssist.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "episode_of_care_diagnosis")
public class EpisodeOfCareDiagnosisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    private EpisodeOfCareEntity episode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_id", nullable = false)
    private ConditionEntity condition;

    @Column(name = "role_system")
    private String roleSystem;

    @Column(name = "role_code", length = 20)
    private String roleCode;

    @Column(name = "role_display", length = 100)
    private String roleDisplay;

    @Column(name = "rank")
    private Integer rank;
}
