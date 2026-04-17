package ehrAssist.entity;

import ehrAssist.entity.master.ObservationCodeMasterEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "vitals")
public class VitalsEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "encounter_id")
    private UUID encounterId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "observation_code_id", referencedColumnName = "item_id")
    private ObservationCodeMasterEntity codeMaster;

    @Column(name = "status")
    private String status;

    @Column(name = "value_quantity", precision = 10, scale = 2)
    private BigDecimal valueQuantity;

    @Column(name = "value_unit")
    private String valueUnit;

    @Column(name = "value_string")
    private String valueString;

    @Column(name = "interpretation_code")
    private String interpretationCode;

    @Column(name = "effective_date")
    private LocalDateTime effectiveDate;
}
