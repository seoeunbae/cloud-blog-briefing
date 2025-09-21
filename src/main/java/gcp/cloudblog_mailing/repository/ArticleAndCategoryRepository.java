package gcp.cloudblog_mailing.repository;

import gcp.cloudblog_mailing.model.entity.ArticleAndCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleAndCategoryRepository extends JpaRepository<ArticleAndCategory, Integer>, ArticleAndCategoryCustom {
    List<ArticleAndCategory> getArticleAndCategoriesById(Integer id);
}
