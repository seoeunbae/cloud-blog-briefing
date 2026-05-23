package gcp.cloudblog_mailing.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "gcp_relationships")
public class GcpRelationship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "relationship_id")
    private Integer relationshipId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_entity_id", referencedColumnName = "entity_id", nullable = false)
    private GcpEntity sourceEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_entity_id", referencedColumnName = "entity_id", nullable = false)
    private GcpEntity targetEntity;

    @Column(name = "relation_type", nullable = false, length = 100)
    private String relationType; // e.g., 'RUNS', 'COMPETITOR_OF', 'IS_A', 'INTEGRATES_WITH'

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
