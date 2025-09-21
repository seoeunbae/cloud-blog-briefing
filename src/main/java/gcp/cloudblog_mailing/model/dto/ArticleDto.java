package gcp.cloudblog_mailing.model.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import gcp.cloudblog_mailing.crawling.CrawlingDto;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class ArticleDto {
    private String title;
    private String summary;
    private String normalSummary;
    private String totalContent;
    private String link;
    private String imgLink;
    private String date;

    public static class ArticleResponse {
        @JsonProperty("articles")
        private List<ArticleDto> articles;

        public List<ArticleDto> getArticles() {
            return articles;
        }
    }

    @JsonCreator
    public static ArticleDto fromMap(
            @JsonProperty("title") String title,
            @JsonProperty("link") String link,
            @JsonProperty("pubDate") String date,
            @JsonProperty("normalSummary") String normalSummary,
            @JsonProperty("summary") String summary,
            @JsonProperty("total_content") String totalContent,
            @JsonProperty("imgLink") String imgLink
    ) {
        return ArticleDto.builder()
                .title(title)
                .link(link)
                .date(date)
                .normalSummary(normalSummary)
                .summary(summary)
                .totalContent(totalContent)
                .imgLink(imgLink)
                .build();
    }

    public static ArticleDto fromMap(Map<String, Object> map) {
        try {
            return ArticleDto.builder()
                    .title((String) map.get("title"))
                    .link((String) map.get("link"))
                    .date((String) map.get("pubDate"))
                    .normalSummary((String) map.get("normalSummary"))
                    .summary((String) map.get("summary"))
                    .totalContent((String) map.get("total_content"))
                    .imgLink((String) map.get("imgLink"))
                    .build();
        } catch (Exception e) {
            log.error("Error converting map to ArticleDto: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid map structure", e);
        }
    }
}
