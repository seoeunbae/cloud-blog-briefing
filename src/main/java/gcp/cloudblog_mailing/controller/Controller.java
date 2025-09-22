package gcp.cloudblog_mailing.controller;

import gcp.cloudblog_mailing.crawling.RssFilter;
import gcp.cloudblog_mailing.crawling.enums.Category;
import gcp.cloudblog_mailing.model.dto.SubscriberDto;
import gcp.cloudblog_mailing.model.entity.Article;
import gcp.cloudblog_mailing.service.MailingService;
import gcp.cloudblog_mailing.util.EmailEncoder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Slf4j
@org.springframework.stereotype.Controller
@RequestMapping("/v1/mail")
@RequiredArgsConstructor
public class Controller {
    private final MailingService service;
    private final RssFilter rssFilter;
    private static final String SOURCE_URL="https://cloudblog.withgoogle.com/rss/";
    private static final String DEFAULT_ROLE = "Customer Engineer";
    private static final Integer DEFAULT_CHARACTER_CNT = 400;
    private final MailingService mailingService;

//    @GetMapping("/{email}/audio")
//    public ResponseEntity<byte[]> getAudio(@PathVariable String email) throws IOException, InterruptedException, ExecutionException {
//        String emailContent = service.getEmailContent(email);
////        byte[] bytes = service.makeAudio(emailContent);
//
//        HttpHeaders headers = new HttpHeaders();
//
//        // 3. Set the Content-Type header to "audio/mpeg" for MP3 files
//        headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
//
//        // 4. Set the Content-Disposition header to trigger a download
//        //    This also suggests a filename to the browser.
//        String filename = "audio_for_" + email.replaceAll("[^a-zA-Z0-9.-]", "_") + ".mp3";
//        headers.setContentDispositionFormData("attachment", filename);
//
//        // 5. Set the Content-Length header (good practice for file downloads)
//        headers.setContentLength(bytes.length);
//
//        // 6. Build and return the ResponseEntity
//        //    This combines the audio data, headers, and an "OK" (200) status
//        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
//    }

    @Scheduled(cron = "0 0 18 * * WED,SUN", zone = "Asia/Seoul")
    @PostMapping("/ai/weekly")
    @ResponseBody
    @Transactional
    public void storeContentWeekly() throws IOException {
        log.info("start crawling weekly");
        mailingService.deleteAll();
        rssFilter.crawlingFromRss(SOURCE_URL); //not personal
    }

    public List<Category> processCategories(String[] categories) {
        List<Category> validCategories = new ArrayList<>();
        for(String category : categories){
            Category enumCate = Category.valueOf(category);
            validCategories.add(enumCate);
        }
        return validCategories;
    }

//    @Scheduled(cron = "0 0 8 * * MON", zone = "Asia/Seoul")
    @PostMapping("/ai/release")
    @ResponseBody
    @Transactional
    public ResponseEntity<String> sendContent() throws ExecutionException, InterruptedException {
        List<SubscriberDto> subscribers = service.getSubscribers();
        for(SubscriberDto sub : subscribers) {
            log.info("filtered email to {}", sub.getEmail());
            if(sub.getCategories() == null){
                sub.setCategories(Category.AI.name());
            }
            if(sub.getCharacterCount() == null){
                sub.setCharacterCount(DEFAULT_CHARACTER_CNT);
            }
            Map<String, List<Article>> article = service.getArticle(sub.getCharacterCount(), sub.getRole(), processCategories(sub.getCategories().split(",")));
            int countOfArticle = 0;
            for(String art : article.keySet()){
                countOfArticle += article.get(art).size();
            }
            String content = service.makeEmailContent(article, sub.getEmail(), sub.getCharacterCount(), sub.getRole(), processCategories(sub.getCategories().split(",")));
            log.info("send email to {}", sub.getEmail());
            String receiver = "";
            if(sub.getEmail().contains("@")){
                receiver = sub.getEmail();
            }else{
                receiver = EmailEncoder.decryptEmail(sub.getEmail());
            }
            log.info("send email to {}", receiver);
            service.sendMail(receiver, content, countOfArticle);
        }
        return ResponseEntity.ok("Newsletter sent to " + subscribers.size() + " subscribers");
    }

    @PostMapping
    @ResponseBody
    public String makeDirectLetter(@RequestBody SubscriberDto subscriberDto) throws ExecutionException, InterruptedException {
        List<gcp.cloudblog_mailing.crawling.enums.Category> categoryList = new ArrayList<>();
        log.info("makeDirectLetter : {}", subscriberDto.toString());
        if(subscriberDto.getCategories()==null){
            subscriberDto.setCategories(Category.AI.name());
        }

        if(subscriberDto.getRole()==null||subscriberDto.getRole().isEmpty()){
            subscriberDto.setRole(DEFAULT_ROLE);
        }

        if(subscriberDto.getCategories() != null){
            String[] splitCate = subscriberDto.getCategories().split(",");
            for(String cate : splitCate){
                log.info("cate : {}", cate);
                categoryList.add(gcp.cloudblog_mailing.crawling.enums.Category.valueOf(cate));
            }
        } else {
            categoryList.add(Category.AI);
        }
        Map<String, List<Article>> articles = service.getArticle(subscriberDto.getCharacterCount(),subscriberDto.getRole(), categoryList);
        int countOfArticle = 0;
        for(String art : articles.keySet()){
            countOfArticle += articles.get(art).size();
        }
        String content = service.makeEmailContent(articles, subscriberDto.getEmail(), subscriberDto.getCharacterCount(), subscriberDto.getRole(), categoryList);
        return service.sendMail(subscriberDto.getEmail(),content, countOfArticle);
    }

    @Transactional
    @PostMapping("/subscribe")
    public String subscribe(@ModelAttribute("subscriber") SubscriberDto subscriber, BindingResult result, Model model
    ){
        if(subscriber.getCategories() == null){
            subscriber.setCategories(Category.AI.name());
        }
        subscriber.setCharacterCount(subscriber.getCharacterCount() == null ? 600 : subscriber.getCharacterCount());
        log.info("subscribe : {}", subscriber.getCharacterCount());
        if (result.hasErrors()) {
            log.error("Validation Error: {}", result.getAllErrors());
            return "form";
        }
        try {
            if(service.isSubscribed(subscriber.getEmail())){
                log.info("already subscribed email : {}", subscriber.getEmail());
                service.subscribe(subscriber);
                model.addAttribute("successMessage", "이미 구독 중인 이메일입니다, 기존 정보를 업데이트 했습니다.");
                return "form";
            }
            service.subscribe(subscriber);
            log.info("subscribe email : {}", subscriber.getEmail());
            model.addAttribute("successMessage", "구독이 완료되었습니다.");
            return "form";
        } catch (Exception e){
            log.error("subscribe error : {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            return "form";
        }
    }

    @GetMapping("/subscribe")
    public String subscribeForm(@ModelAttribute("subscriber") SubscriberDto subscriber, Model model, @RequestParam(required = false) String status){
        if ("success".equals(status)){
            model.addAttribute("successMessage", "구독이 완료되었습니다.");
            return "form";
        }else if("error".equals(status)){
            model.addAttribute("errorMessage", "구독 처리 중 오류가 발생했습니다.");
        }else {
            model.addAttribute("subscriber", SubscriberDto.builder().build());
        }
        return "form";
    }

    @GetMapping("/archive/{email}")
    public String archive(Model model, @PathVariable(name = "email") String email) throws ExecutionException, InterruptedException {
        try {
            String decryptedEmail = EmailEncoder.decryptEmail(email);
            TreeMap<String, String> articles = service.getAllArticles(decryptedEmail);
            log.info("email : {} "+ decryptedEmail);
            model.addAttribute("email", decryptedEmail);
            model.addAttribute("articles", articles);
            return "archive"; // Corresponds to archive.html in templates
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Unable to retrieve articles");
            return "error";
        }

    }
}
