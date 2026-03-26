package ehrAssist.entity.master;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "medication_code_master")
public class MedicationCodeMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code_system")
    private String codeSystem;

    @Column(name = "code_value", unique = true)
    private String codeValue;

    @Column(name = "code_display")
    private String codeDisplay;

    @Column(name = "generic_name")
    private String genericName;

    @Column(name = "form_code")
    private String formCode;

    @Column(name = "form_display")
    private String formDisplay;

    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;
}
