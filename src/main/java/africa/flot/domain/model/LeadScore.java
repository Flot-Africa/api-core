package africa.flot.domain.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lead_scores")
@Getter
@Setter
public class LeadScore extends PanacheEntityBase {
    @Id
    private UUID id;

    @Column(name = "lead_id")
    private UUID leadId;

    @Column(name = "personal_data_score")
    private double personalDataScore;

    @Column(name = "vtc_experience_score")
    private double vtcExperienceScore;

    @Column(name = "driving_record_score")
    private double drivingRecordScore;

    @Column(name = "total_score")
    private double totalScore;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
