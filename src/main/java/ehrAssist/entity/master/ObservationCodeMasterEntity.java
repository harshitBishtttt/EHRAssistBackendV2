package ehrAssist.entity.master;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "observation_code_master")
public class ObservationCodeMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "item_id", unique = true)
    private Integer itemId;

    @Column(name = "code_system")
    private String codeSystem;

    @Column(name = "loinc_code")
    private String loincCode;

    @Column(name = "code_display")
    private String codeDisplay;

    @Column(name = "category")
    private String category;

    @Column(name = "fhir_category_code")
    private String fhirCategoryCode;

    @Column(name = "fluid")
    private String fluid;

    @Column(name = "expected_unit")
    private String expectedUnit;

    @Column(name = "reference_range_low")
    private BigDecimal referenceRangeLow;

    @Column(name = "reference_range_high")
    private BigDecimal referenceRangeHigh;

    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;
}
