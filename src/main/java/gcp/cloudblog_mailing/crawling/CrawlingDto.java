package gcp.cloudblog_mailing.crawling;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
public class CrawlingDto {
    String id ;

    String title;

    String content;

    String link ;

    String description ;
//    String imgLink;
    List<String> categories;
    String pubDate;
//    ArrayList<Element> roleOfAuthor;

    public static CrawlingDto mapToCrawlingDto(Object sourceObject) {
        // If sourceObject is a custom object
        if (sourceObject instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) sourceObject;
            return CrawlingDto.builder()
                    .id(LocalDateTime.now() + " " + map.get("pubDate"))
                    .title((String) map.get("title"))
                    .link((String) map.get("link"))
                    .content((String) map.get("content"))
                    .pubDate((String) map.get("pubDate"))
                    .description((String) map.get("description"))
                    .categories(parseCategories(map.get("categories")))
                    .build();

        }

        if (sourceObject instanceof CrawlingDto) {
            CrawlingDto source = (CrawlingDto) sourceObject;
            return CrawlingDto.builder()
                    .id(source.getId())
                    .title(source.getTitle())
                    .link(source.getLink())
                    .pubDate(source.getPubDate())
                    .description(source.getDescription())
                    .categories(source.getCategories())
                    .build();
        }
        throw new IllegalArgumentException("Unsupported object type for serialization");
    }

    private static List<String> parseCategories(Object categoriesObj) {
        if (categoriesObj == null) return Collections.emptyList();

        if (categoriesObj instanceof List) {
            return (List<String>) categoriesObj;
        }

        if (categoriesObj instanceof String) {
            // If categories are comma-separated
            return Arrays.stream(((String) categoriesObj).split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

}
