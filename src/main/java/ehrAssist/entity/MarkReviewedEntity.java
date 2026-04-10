package ehrAssist.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mark_reviewed")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MarkReviewedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;
    @Column(name = "is_reviewed", nullable = false)
    private Boolean isReviewed;
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
}