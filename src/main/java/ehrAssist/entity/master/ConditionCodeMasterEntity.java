package ehrAssist.entity.master;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "condition_code_master")
public class ConditionCodeMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code_system")
    private String codeSystem;

    @Column(name = "code_value", columnDefinition = "NCHAR(10)")
    private String codeValue;

    @Column(name = "short_title")
    private String shortTitle;

    @Column(name = "long_title")
    private String longTitle;

    @Column(name = "category")
    private String category;

    @Column(name = "category_code")
    private String categoryCode;

    @Column(name = "icd10_code")
    private String icd10Code;

    @Column(name = "snomed_code")
    private String snomedCode;

    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;
}
