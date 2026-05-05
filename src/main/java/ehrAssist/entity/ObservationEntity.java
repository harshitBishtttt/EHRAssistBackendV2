package ehrAssist.entity;

import ehrAssist.entity.master.ObservationCodeMasterEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "observation")
public class ObservationEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encounter_id")
    private EncounterEntity encounter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performer_id")
    private PractitionerEntity performer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "observation_code_id", referencedColumnName = "item_id")
    private ObservationCodeMasterEntity codeMaster;

    @Column(name = "status")
    private String status;

    @Column(name = "value_quantity", precision = 18, scale = 4)
    private BigDecimal valueQuantity;

    @Column(name = "value_unit")
    private String valueUnit;

    @Column(name = "value_string")
    private String valueString;

    @Column(name = "interpretation_code")
    private String interpretationCode;

    @Column(name = "effective_date")
    private LocalDateTime effectiveDate;

    @Column(name = "issued")
    private LocalDateTime issued;
}
