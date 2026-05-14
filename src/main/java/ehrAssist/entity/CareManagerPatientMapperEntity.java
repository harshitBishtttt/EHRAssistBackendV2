package ehrAssist.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "care_manager_patient_mapper")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareManagerPatientMapperEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "care_manager_id", nullable = false)
    private UUID careManagerId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;
}
