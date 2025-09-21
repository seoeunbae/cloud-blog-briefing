package gcp.cloudblog_mailing.crawling;

import gcp.cloudblog_mailing.model.entity.Article;
import gcp.cloudblog_mailing.model.entity.ArticleAndCategory;
import gcp.cloudblog_mailing.model.entity.Category;
import gcp.cloudblog_mailing.repository.ArticleAndCategoryRepository;
import gcp.cloudblog_mailing.repository.ArticleRepository;
import gcp.cloudblog_mailing.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class RssFilter {
    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final ArticleAndCategoryRepository articleAndCategoryRepository;

    final LocalDate startDate = LocalDate.now().minusDays(8);
    final LocalDate endDate = LocalDate.now();

    private HashMap<String, String> makeCategoryMap(){
        HashMap<String, String> map = new HashMap<>();
        map.put("AI Hypercomputer","AI & Machine Learning");
        map.put("AI & Machine Learning","AI & Machine Learning");
        map.put("Security & Identity", "Security");
        map.put("Threat Intelligence", "Security");
        map.put("Databases", "Database");
        map.put("Data Analytics", "Data Analytics");
        map.put("Management Tools", "Infrastructure");
        map.put("Serverless", "Infrastructure");
        map.put("Application Development", "Infrastructure");
        map.put("Bigquery", "Data Analytics");
        map.put("Application Modernization", "Application Modernization");
        map.put("Compute", "Infrastructure");
        map.put("Telecommunications", "Network");
        map.put("Network", "Network");
        map.put("Cloud CISO", "Cloud");
        map.put("Storage & Data Transfer", "Database");
        return map;
    }

    public void crawlingFromRss(String url) throws IOException {
        log.info("crawling from rss : {}", url);
        HashMap<String, String> categoryMap = makeCategoryMap();
        Document doc = Jsoup.connect(url)
                .parser(Parser.xmlParser())
                .get();

        Elements items = doc.select("item");

        for(Element item : items){
            String pubDateStr = item.select("pubDate").text();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z")
                    .withLocale(Locale.ENGLISH);
            LocalDate pubDate = ZonedDateTime.parse(pubDateStr.trim(), formatter).toLocalDate();

            if (pubDate.isAfter(startDate) & pubDate.isBefore(endDate) ) {
                String title = item.select("title").first().text();
                String content = item.select("description").first().text();
                String link = item.select("link").first().text();
                String author = item.select("author").first().text();
                ArrayList<Element> category = item.select("category").asList();
                String categoriesString = item.select("category").eachText().stream()
                        .collect(Collectors.joining(", "));
                String titleCategory = "";
                for(Element categoryElement : category){
                    titleCategory = categoryMap.getOrDefault(categoryElement.text(), "Other");
                    if(!titleCategory.equals("Other")){
                        break;
                    }
                }
                Article article = Article.fromRss(title, titleCategory ,content, link, null, pubDate, author, categoriesString);
                Article savedArticle = articleRepository.save(article);

                for(Element categoryElement : category){
                    String categoryName = categoryElement.text();
                    Category each = new Category();
                    each.setCategoryName(categoryName);
                    titleCategory = categoryMap.getOrDefault(categoryElement.text(), "Other");
                    each.setTitleCategory(titleCategory);
                    if(!categoryRepository.existsByCategoryName(categoryName)){
                        Category savedCate = categoryRepository.save(each);

                        ArticleAndCategory articleAndCategory = new ArticleAndCategory();
                        articleAndCategory.setArticleId(savedArticle);
                        articleAndCategory.setCategoryId(savedCate);

                        articleAndCategoryRepository.save(articleAndCategory);
                    } else {
                        Category cate = categoryRepository.findByCategoryName(categoryName).orElseThrow();

                        ArticleAndCategory articleAndCategory = new ArticleAndCategory();
                        articleAndCategory.setCategoryId(cate);
                        articleAndCategory.setArticleId(savedArticle);
                        articleAndCategoryRepository.save(articleAndCategory);
                    }
                }
            }
        }
    }

}
