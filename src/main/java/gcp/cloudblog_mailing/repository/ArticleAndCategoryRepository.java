package gcp.cloudblog_mailing.repository;

import gcp.cloudblog_mailing.model.entity.ArticleAndCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ArticleAndCategoryRepository extends JpaRepository<ArticleAndCategory, Integer>, ArticleAndCategoryCustom {
    List<ArticleAndCategory> getArticleAndCategoriesById(Integer id);
}
