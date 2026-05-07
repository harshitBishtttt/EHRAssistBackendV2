package ehrAssist.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ai_recommendation_payload")
public class AiRecommendationPayloadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AiRecommendationEntity recommendation;

    @Column(name = "sequence", nullable = false)
    private Integer sequence;

    @Column(name = "content_string", nullable = false, length = 2000)
    private String contentString;
}
