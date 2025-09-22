package gcp.cloudblog_mailing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.transaction.annotation.Propagation;
import java.io.IOException;import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.genai.Client;
import com.google.genai.types.*;
import gcp.cloudblog_mailing.crawling.enums.Category;
import gcp.cloudblog_mailing.model.dto.ArticleDto;
import gcp.cloudblog_mailing.model.dto.SubscriberDto;
import gcp.cloudblog_mailing.model.entity.Article;
import gcp.cloudblog_mailing.repository.ArticleAndCategoryRepository;
import gcp.cloudblog_mailing.repository.ArticleRepository;
import gcp.cloudblog_mailing.util.EmailEncoder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mapping.AccessOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.sound.sampled.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailingService extends Command{
    private final ArticleAndCategoryRepository articleAndCategoryRepository;

    @Value("${ai.studio.key}") private String ai_studio_key;

    @Value("${mail.username}") private String emailId;

    @Value("${mail.password}") private String emailPw;

    private final SpringTemplateEngine templateEngine;
    private final Firestore firestore;
    private final ArticleRepository articleRepository;
    private final ExecutorService renderingExecutor = Executors.newSingleThreadExecutor();
    private static final String COLLECTION_NAME = "subscribers";
    private static final String PORT = "465";
    private static final String SMTPHOST="smtp.gmail.com";
    private static final String ARTICLE_COLLECTION_NAME="articles";

//    private final Client client = Client.builder().apiKey(ai_studio_key).build();

//    public void makeAudio(){
//        String apiKey = "YOUR_API_KEY"; // 본인의 API 키로 변경
//        String modelName = "gemini-2.5-flash-preview-tts"; // TTS 지원 모델
//        String voiceName = "Puck"; // 사용할 음성 (예: Puck, Kore, Leda...)
//        String prompt = "Say cheerfully: 안녕하세요! 자바로 Gemini TTS를 테스트 중입니다.";
//        String outputFilePath = "output_java.wav";
//
//            // 2. 음성 설정 (VoiceConfig)
//        PrebuiltVoiceConfig prebuiltVoiceConfig = PrebuiltVoiceConfig.builder()
//                .setVoiceName(voiceName)
//                .build();
//
//        VoiceConfig voiceConfig = VoiceConfig.builder()
//                .setPrebuiltVoiceConfig(prebuiltVoiceConfig)
//                .build();
//
//        // 3. 스피치 설정 (SpeechConfig)
//        SpeechConfig speechConfig = SpeechConfig.builder()
//                .setVoiceConfig(voiceConfig)
//                .build();
//
//        // 4. 생성 설정 (GenerationConfig) - 오디오 모달리티 지정
//        GenerationConfig genConfig = GenerationConfig.newBuilder()
//                .addResponseModalities(ResponseModality.AUDIO) // 오디오 출력 요청
//                .setSpeechConfig(speechConfig)
//                .build();
//
//        // 5. 모델 초기화
//        GenerativeModel model = new GenerativeModel(modelName, apiKey);
//
//        // 6. 콘텐츠 생성 요청 (프롬프트와 생성 설정을 함께 전달)
//        System.out.println("'" + prompt + "' 음성 생성을 요청합니다...");
//        GenerateContentResponse response = client.model.async.generateContent(prompt, genConfig);
//
//        // 7. 오디오 데이터 추출
//        Part audioPart = response.getCandidates(0).getContent().getParts(0);
//        ByteString audioData = audioPart.getInlineData().getData();
//        byte[] pcmData = audioData.toByteArray();
//
//        System.out.println("오디오 데이터를 받았습니다. 파일로 저장 중...");
//
//        // 8. Raw PCM 데이터를 .wav 파일로 저장
//        savePcmAsWav(pcmData, outputFilePath, 24000, 16, 1);
//
//        System.out.println("성공! '" + outputFilePath + "' 파일로 저장되었습니다.");
//    }

//    private static void savePcmAsWav(byte[] pcmData, String filePath, float sampleRate, int sampleSizeInBits, int channels)
//            throws IOException, UnsupportedAudioFileException {
//
//        // 오디오 포맷 설정 (PCM, 24kHz, 16bit, mono, signed, little-endian)
//        AudioFormat format = new AudioFormat(
//                AudioFormat.Encoding.PCM_SIGNED,
//                sampleRate,
//                sampleSizeInBits,
//                channels,
//                (sampleSizeInBits / 8) * channels, // frameSize
//                sampleRate, // frameRate
//                false // bigEndian
//        );
//
//        ByteArrayInputStream bais = new ByteArrayInputStream(pcmData);
//        AudioInputStream audioInputStream = new AudioInputStream(bais, format, pcmData.length / format.getFrameSize());
//
//        java.io.File outputFile = new File(filePath);
//        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);
//
//        audioInputStream.close();
//        bais.close();
//    }

    public void deleteAll(){
        log.info("delete all articles");
        articleAndCategoryRepository.deleteAll();
        articleRepository.deleteAll();
    }

    @Transactional
    public Article processCMD(String cmd, Article sourceArticle){
        Client client = Client.builder().apiKey(ai_studio_key).build();
        log.info("cmd : {}", cmd);
        Schema articleSchema =
                Schema.builder()
                        .type(Type.Known.OBJECT)
                        .description("A single detailed article information")
                        .properties(
                                ImmutableMap.<String, Schema>builder()
                                        .put("title", Schema.builder().type(Type.Known.STRING).build())
                                        .put("summary", Schema.builder().type(Type.Known.STRING).description("한국어로 주어진 직무의 관점에서의 요약문을 만들어줘").build())
                                        .put("totalContent", Schema.builder().type(Type.Known.STRING).description("한국어로 전체내용을 50줄 가량의 마크다운으로 요약").build())
                                        .put("normalSummary",  Schema.builder().type(Type.Known.STRING).description("한국어로 전체내용을 특정한 관점이 아닌, 일반적으로 50줄 가량으로 요약, 마크다운으로 구성").build())
                                        .put("link", Schema.builder().type(Type.Known.STRING).build())
                                        .put("date", Schema.builder().type(Type.Known.STRING).build())
                                        .put("imgLink", Schema.builder().type(Type.Known.STRING).build())
                                        .build())
                        .required(ImmutableList.of( "title", "summary", "totalContent", "normalSummary", "link", "date", "imgLink"))
                        .build();

        Schema responseSchema =
                Schema.builder()
                        .type(Type.Known.OBJECT)
                        .properties(
                                ImmutableMap.of(
                                        "articles",
                                        Schema.builder()
                                                .type(Type.Known.ARRAY)
                                                .items(articleSchema) // Nest the recipe schema here
                                                .build()))
                        .required(ImmutableList.of("articles"))
                        .build();

        GenerateContentConfig config =
                GenerateContentConfig.builder()
                        .responseSchema(responseSchema)
                        .responseMimeType("application/json")
//                        .responseJsonSchema(responseSchema)
//                        .tools(urlContextTool)
                        .build();

        GenerateContentResponse response =
                client.models.generateContent("gemini-2.5-pro", cmd, config);

        log.info(response.text());
        ObjectMapper objectMapper = new ObjectMapper();
        ArticleDto.ArticleResponse articleResponse = null;
        try {
            articleResponse = objectMapper.readValue(response.text(), ArticleDto.ArticleResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        List<ArticleDto> articles = articleResponse.getArticles();
        ArticleDto article = articles.get(0);
        log.info("articles : {}", articles.size());
        log.info("article_id:{}", article.getLink());
        return updateArticle(sourceArticle, article);
    }

    @Transactional
    public Article updateArticle(Article sourceArticle, ArticleDto article){
        Parser parser = org.commonmark.parser.Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        sourceArticle.setTotalContent(renderer.render(parser.parse(article.getTotalContent())));
        sourceArticle.setSummary(renderer.render(parser.parse(article.getSummary())));
        sourceArticle.setNormalSummary(renderer.render(parser.parse(article.getNormalSummary())));
        return sourceArticle;
    }

    public Map<String, List<Article>> getArticle(Integer character_cnt, String role, List<Category> categories) {
        Map<String, List<Article>> map = new HashMap<>();
        for(gcp.cloudblog_mailing.crawling.enums.Category category : categories){
            log.info("category : {}", category.getCategoryName());
            List<Article> allByTitleCategory = articleRepository.findArticleByTitleCategory(category.getCategoryName());
            for(Article article : allByTitleCategory){
                articleRepository.getArticleById(article.getId());
                if(article.getTotalContent() == null){
                    String cmd = makeCmd(article, character_cnt, role, category);
                    Article updatedArticle = processCMD(cmd, article);
                    articleRepository.save(updatedArticle);
                    allByTitleCategory.set(allByTitleCategory.indexOf(article), updatedArticle);
                }
            }
            map.put(category.getCategoryName(), allByTitleCategory);
        }
        log.info("map : {}", map);
        return map;
    }

    public Session setting(Properties props){
        Session session= null;
        try {
            props.put("mail.smtp.host", SMTPHOST);
            props.put("mail.smtp.auth", true);
            props.put("mail.smtp.port", PORT);
            props.put("mail.smtp.ssl.enable", true);
            props.put("mail.smtp.ssl.trust", SMTPHOST);
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");

            props.put("mail.smtp.quit-wait", false);
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", false);
            props.put("mail.smtp.socketFactory.port", PORT);

            session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(emailId, emailPw);
                }
            });

    } catch(Exception e) {
            log.error(e.getMessage());
        }
        log.info("session : {}", session);
        return session;
    }

    @Transactional
    public String makeEmailContent(Map<String, List<Article>> content, String email, int character_cnt, String role, List<Category> categories) {
        String decryptedEmail = "";
        if(email.contains("@")){
            decryptedEmail = email;
        }else{
            decryptedEmail = EmailEncoder.decryptEmail(email);
        }
        if(role == null || role.isEmpty()){
            role = "Customer Engineer Team";
        }
        log.info("make content to {}", decryptedEmail);
        Context context = new Context();
        log.info("content : {}", content);
        context.setVariable("articles", content);;
        log.info("article.categories : {}", content);
        context.setVariable("categories", categories);
        log.info("Categories: {}", categories);
        context.setVariable("role", role);
        context.setVariable("characterCount", character_cnt);
        context.setVariable("email", decryptedEmail);
        String encodedEmail = EmailEncoder.encryptEmail(email);
        context.setVariable("encodedEmail", encodedEmail);
        context.setVariable("webVersionLink", "https://cloud.google.com/blog/products/ai-machine-learning");
        String emailContent = templateEngine.process("template2", context);
        log.info("email content stored : {}", emailContent);
        return emailContent;
    }

    public String sendMail(String email, String emailContent, int countOfArticle) throws ExecutionException, InterruptedException {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
        DocumentReference docRef = firestore.collection("articlesOfWeek")
                .document(email)
                .collection("dates")
                .document(monday.format(DateTimeFormatter.ISO_LOCAL_DATE).toString());

        Map<String, Object> data = new HashMap<>();
        data.put("content", emailContent);
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());
        data.put("role", "Customer Engineer");
        data.put("audio", null);
        docRef.set(data).get();
        return CompletableFuture.supplyAsync(() -> {
            log.info("mailing service :send mail to  {} ", email);
            try {
                Message message = new MimeMessage(setting(new Properties()));
                String receiverId = email;
                String subject = "[CLOUD BLOG] GCP weekly mail for Customer Engineer";
                message.setFrom(new InternetAddress(emailId));
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(receiverId));
                message.setSubject(subject);
                message.setContent(emailContent, "text/html; charset=utf-8");
                Transport.send(message);
                log.info("Email sent to {}", receiverId);
            } catch (MessagingException e) {
                log.info(e.getMessage());
            }
            log.info("! {} ,  {}", emailId, emailPw);
            return emailContent;
        }, renderingExecutor).join();
    }

    public void subscribe(SubscriberDto subscriberDto) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME)
                .document(subscriberDto.getEmail());
        Timestamp now = Timestamp.now();

        Map<String, Object> newData = new HashMap<>();
        String encodedEmail = EmailEncoder.encryptEmail(subscriberDto.getEmail());
        newData.put("email", encodedEmail);
        newData.put("characterCount", subscriberDto.getCharacterCount());
        newData.put("role", subscriberDto.getRole());
        newData.put("categories", subscriberDto.getCategories());

        DocumentSnapshot document = docRef.get().get();
        if (document.exists()) {
            Timestamp createdAt = document.getTimestamp("createdAt");
            newData.put("createdAt", createdAt);
            newData.put("updatedAt", now);
        } else {
            newData.put("createdAt", now);
        }
        docRef.set(newData).get();
    }

    public String getEmailContent(String email) throws IOException, InterruptedException, ExecutionException {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
        DocumentReference docRef = firestore.collection(ARTICLE_COLLECTION_NAME)
                .document("email")
                .collection(email)
                .document("dates")
                .collection(monday.format(DateTimeFormatter.ISO_LOCAL_DATE).toString())
                .document("content");

        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();

        if (document.exists()) {
            Map<String, Object> data = document.getData();
           System.out.println("문서를 성공적으로 찾았습니다. 데이터: ");
            return data.get("content").toString();
        } else {
            System.out.println("해당 경로에 문서가 존재하지 않습니다: " + docRef.getPath());
            return null;
        }
    }

    public void unsubscribe(String email) {
        try {
            firestore.collection(COLLECTION_NAME)
                    .document(email)
                    .delete()
                    .get(); // 동기적 처리를 위해 get() 호출
        } catch (Exception e) {
            throw new RuntimeException("구독 취소 중 오류가 발생했습니다.", e);
        }
    }

    public List<SubscriberDto> getSubscribers() {
        try {
            List<SubscriberDto> collect = firestore.collection(COLLECTION_NAME)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(doc -> SubscriberDto.fromMap(doc.getData()))
                    .collect(Collectors.toList());
            log.info("getSubscribers : {}", collect.size());
            return collect;
        } catch (Exception e) {
            throw new RuntimeException("구독자 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    public boolean isSubscribed(String email) {
        try {
            return firestore.collection(COLLECTION_NAME)
                    .document(email)
                    .get()
                    .get()
                    .exists();
        } catch (Exception e) {
            throw new RuntimeException("구독 상태 확인 중 오류가 발생했습니다.", e);
        }
    }

    public String getEmailContent(SubscriberDto subscriber) throws IOException, InterruptedException, ExecutionException {
        LocalDate now = LocalDate.now();
        LocalDate monday = now.with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
        String documentId = monday.format(DateTimeFormatter.ISO_LOCAL_DATE).toString(); // yyyy-MM-dd 형식
        log.info("documentId : {}", documentId);
        DocumentReference docRef = firestore.collection(ARTICLE_COLLECTION_NAME)
                .document("dates")
                .collection(documentId)
                .document("content");

        DocumentSnapshot document = docRef.get().get();

        if (document.exists()) {
            String content = document.getString("content");
            if (content == null || content.isEmpty()) {
                log.error("Content is empty for email: {} on date: {}", subscriber.getEmail(), documentId);
                throw new RuntimeException("뉴스레터 컨텐츠가 비어있습니다");
            }
            return content;
        } else {
            log.error("No document found for email: {} on date: {}", subscriber.getEmail(), documentId);
            throw new RuntimeException("해당 날짜의 뉴스레터를 찾을 수 없습니다");
        }
    }

    public Map<String, List<Article>> getArticlesByCategory(List<gcp.cloudblog_mailing.crawling.enums.Category> categories) throws ExecutionException, InterruptedException {
        LocalDate now = LocalDate.now();
        LocalDate day = now.minusDays(7);
        log.info(day.toString());
        String documentId = day.format(DateTimeFormatter.ISO_LOCAL_DATE).toString(); // yyyy-MM-dd 형식
        log.info("documentId : {}", documentId);
        Map<String, List<Article>> map = new HashMap<>();
        for(gcp.cloudblog_mailing.crawling.enums.Category category : categories){
            List<Article> allByTitleCategory = articleRepository.findAllByTitleCategory(category.getCategoryName());
            map.put(category.getCategoryName(), allByTitleCategory);
        }
        return map;
    }

    public TreeMap<String, String> getAllArticles(String email) throws ExecutionException, InterruptedException {
        TreeMap<String, String> allArticles = new TreeMap<>(Comparator.reverseOrder());
        ApiFuture<QuerySnapshot> future = firestore.collection("articlesOfWeek")
                .document(email)
                .collection("dates")
                .get();

        QuerySnapshot dateDocuments = future.get();

        for (QueryDocumentSnapshot dateDoc : dateDocuments) {
            String date = dateDoc.getId();
            log.info("date : {}", date);
            Object content = dateDoc.getData().get("content");
            log.info("content : {}", content);
            String role = (String) dateDoc.getData().get("role");
            if (content != null) {
                allArticles.put(date, content.toString());
            }
        }
        return allArticles;
    }

    public void checkSingleSubscribe(String email) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME)
                .document(email);
        DocumentSnapshot documentSnapshot = docRef.get().get();

        if(documentSnapshot.getData() == null) {
            throw new RuntimeException("해당 구독자 정보가 존재하지 않습니다.");
        }
    }
}
