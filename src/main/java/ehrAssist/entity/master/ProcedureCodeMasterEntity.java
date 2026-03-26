package ehrAssist.entity.master;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "procedure_code_master")
public class ProcedureCodeMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code_system")
    private String codeSystem;

    @Column(name = "category")
    private Integer category;

    @Column(name = "section_range")
    private String sectionRange;

    @Column(name = "section_header")
    private String sectionHeader;

    @Column(name = "subsection_range")
    private String subsectionRange;

    @Column(name = "subsection_header")
    private String subsectionHeader;

    @Column(name = "fhir_category_code")
    private String fhirCategoryCode;

    @Column(name = "code_suffix")
    private Boolean codeSuffix;

    @Column(name = "min_code")
    private Integer minCode;

    @Column(name = "max_code")
    private Integer maxCode;

    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;
}
