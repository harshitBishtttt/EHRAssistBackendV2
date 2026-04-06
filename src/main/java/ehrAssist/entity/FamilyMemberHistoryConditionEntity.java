package ehrAssist.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "family_member_history_condition")
public class FamilyMemberHistoryConditionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_member_history_id", nullable = false)
    private FamilyMemberHistoryEntity familyMemberHistory;

    @Column(name = "code_system", length = 200)
    private String codeSystem;

    @Column(name = "code_value", nullable = false, length = 20)
    private String codeValue;

    @Column(name = "code_display", length = 200)
    private String codeDisplay;

    @Column(name = "outcome_code", length = 50)
    private String outcomeCode;

    @Column(name = "outcome_display", length = 200)
    private String outcomeDisplay;

    @Column(name = "contributed_to_death")
    private Boolean contributedToDeath;

    @Column(name = "onset_age_value", precision = 5, scale = 1)
    private BigDecimal onsetAgeValue;

    @Column(name = "onset_age_unit", length = 10)
    private String onsetAgeUnit;

    @Column(name = "onset_string", length = 200)
    private String onsetString;

    @Column(name = "note", columnDefinition = "NVARCHAR(MAX)")
    private String note;
}
