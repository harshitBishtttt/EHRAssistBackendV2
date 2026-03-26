package ehrAssist.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "document_reference")
public class DocumentReferenceEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encounter_id")
    private EncounterEntity encounter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private PractitionerEntity author;

    @Column(name = "status")
    private String status;

    @Column(name = "type_code")
    private String typeCode;

    @Column(name = "type_display")
    private String typeDisplay;

    @Column(name = "description")
    private String description;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "content_url")
    private String contentUrl;

    @Column(name = "content_title")
    private String contentTitle;

    @Lob
    @Column(name = "content_data")
    private byte[] contentData;

    @Column(name = "period_start")
    private LocalDateTime periodStart;

    @Column(name = "period_end")
    private LocalDateTime periodEnd;

    @Column(name = "date")
    private LocalDateTime date;
}
