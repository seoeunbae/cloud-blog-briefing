package gcp.cloudblog_mailing.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class ArticleAndCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @JoinColumn(name = "article_id")
    @ManyToOne(fetch = FetchType.LAZY)
    Article articleId;

    @JoinColumn(name = "category_id")
    @ManyToOne(fetch = FetchType.LAZY)
    Category categoryId;
}
