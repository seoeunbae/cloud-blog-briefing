package gcp.cloudblog_mailing.repository;

import gcp.cloudblog_mailing.model.entity.GcpEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GcpEntityRepository extends JpaRepository<GcpEntity, String> {
    Optional<GcpEntity> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
