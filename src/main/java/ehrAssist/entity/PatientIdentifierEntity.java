package ehrAssist.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "patient_identifier")
public class PatientIdentifierEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @Column(name = "system", length = 200)
    private String system;

    @Column(name = "value", length = 100)
    private String value;

    @Column(name = "type_code", length = 10)
    private String typeCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
