package gcp.cloudblog_mailing.repository;

import gcp.cloudblog_mailing.model.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Integer> {
    Optional<Article> getArticleById(Integer id);
    List<Article> findAllByTitleCategory(String titleCategory);
    List<Article> findArticleByTitleCategory(String titleCategory);
}
