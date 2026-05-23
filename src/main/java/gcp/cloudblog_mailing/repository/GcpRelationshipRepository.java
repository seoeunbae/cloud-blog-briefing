package gcp.cloudblog_mailing.repository;

import gcp.cloudblog_mailing.model.entity.GcpRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GcpRelationshipRepository extends JpaRepository<GcpRelationship, Integer> {
    
    @Query("SELECT r FROM GcpRelationship r JOIN FETCH r.sourceEntity JOIN FETCH r.targetEntity " +
           "WHERE r.sourceEntity.entityId = :entityId OR r.targetEntity.entityId = :entityId")
    List<GcpRelationship> findByEntityId(@Param("entityId") String entityId);

    @Query("SELECT r FROM GcpRelationship r JOIN FETCH r.sourceEntity JOIN FETCH r.targetEntity " +
           "WHERE r.sourceEntity.entityId IN :entityIds OR r.targetEntity.entityId IN :entityIds")
    List<GcpRelationship> findByEntityIds(@Param("entityIds") List<String> entityIds);

    @Query("SELECT r FROM GcpRelationship r JOIN FETCH r.sourceEntity JOIN FETCH r.targetEntity " +
           "WHERE r.sourceEntity.name ILIKE %:keyword% OR r.targetEntity.name ILIKE %:keyword%")
    List<GcpRelationship> findByEntityNameKeyword(@Param("keyword") String keyword);
}
