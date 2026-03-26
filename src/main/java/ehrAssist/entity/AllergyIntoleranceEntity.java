package ehrAssist.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "allergy_intolerance")
public class AllergyIntoleranceEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorder_id")
    private PractitionerEntity recorder;

    @Column(name = "clinical_status")
    private String clinicalStatus;

    @Column(name = "verification_status")
    private String verificationStatus;

    @Column(name = "type")
    private String type;

    @Column(name = "category")
    private String category;

    @Column(name = "criticality")
    private String criticality;

    @Column(name = "code_system")
    private String codeSystem;

    @Column(name = "code_value")
    private String codeValue;

    @Column(name = "code_display")
    private String codeDisplay;

    @Column(name = "onset_date")
    private LocalDate onsetDate;
}
