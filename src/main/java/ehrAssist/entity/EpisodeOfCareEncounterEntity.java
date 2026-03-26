package ehrAssist.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "episode_of_care_encounter")
@IdClass(EpisodeOfCareEncounterPk.class)
public class EpisodeOfCareEncounterEntity {

    @Id
    @Column(name = "episode_id", nullable = false)
    private java.util.UUID episodeId;

    @Id
    @Column(name = "encounter_id", nullable = false)
    private java.util.UUID encounterId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
