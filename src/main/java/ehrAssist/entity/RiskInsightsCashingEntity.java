package ehrAssist.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "risk_insights_cashing")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskInsightsCashingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "last_observation_date", nullable = false)
    private LocalDateTime lastObservationDate;

    @Column(name = "last_condition_date", nullable = false)
    private LocalDate lastConditionDate;

    @Column(name = "report_html", columnDefinition = "nvarchar(max)")
    private String reportHtml;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}