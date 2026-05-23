package gcp.cloudblog_mailing.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "gcp_entities")
public class GcpEntity {
    @Id
    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // e.g., 'Service', 'Concept', 'Feature'

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
