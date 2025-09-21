package gcp.cloudblog_mailing.model.dto;

import gcp.cloudblog_mailing.crawling.enums.Category;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@Data
@Slf4j
public class SubscriberDto {
    private String email;
    private Integer characterCount;
    private String role;
    private String categories;

    public static SubscriberDto fromMap(Map<String, Object> map) {
        String categories = (String) map.getOrDefault("category", Category.AI.toString());
        log.info("categories : {}", categories);
        return SubscriberDto.builder()
                .email((String) map.get("email"))
                .role((String) map.get("role"))
                .characterCount(600)
                .categories(categories != null ? categories : Category.AI.toString())
                .build();
    }

}