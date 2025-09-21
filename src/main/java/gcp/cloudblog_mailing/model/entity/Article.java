package gcp.cloudblog_mailing.model.entity;

import com.mysql.cj.jdbc.Blob;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Slf4j
@Entity
@Builder
@Data
@AllArgsConstructor
public class Article {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    @Column(length = 4000)
    String summary;
    @Column
    String title;
    @Column(name = "normal_summary", length = 65535, columnDefinition = "LONGTEXT")
    String normalSummary;
    @Column(name = "total_content" ,length = 500000, columnDefinition = "LONGTEXT")
    String totalContent;
    @Column(length = 500000, columnDefinition = "LONGTEXT")
    String description;
    @Column
    String link;
    @Column(name = "img_link")
    String imgLink;
    @Column(name = "pub_date")
    LocalDate pubDate;
    @Column
    String titleCategory;
    @Column
    String author;
    @Column
    String categories;

    public Article() {
    }

    public static Article fromRss(String title, String titleCategory, String description, String link, String imgLink, LocalDate pubDate, String author, String categories) {
        return Article.builder()
                .title(title)
                .author(author)
                .description(description)
                .link(link)
                .titleCategory(titleCategory)
                .imgLink(imgLink)
                .pubDate(pubDate)
                .categories(categories)
                .build();
    }

    public void updateNormalSummary(String normalSummary) {
        this.normalSummary = normalSummary;
    }
    public void updateTotalContent(String totalContent) {
        this.totalContent = totalContent;
    }
    public void updateSummary(String summary) {
        this.summary = summary;
    }
    public void updateCategories(String categories) {
        this.categories = categories;
    }
}