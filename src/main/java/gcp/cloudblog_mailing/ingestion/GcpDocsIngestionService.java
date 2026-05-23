package gcp.cloudblog_mailing.ingestion;

import gcp.cloudblog_mailing.model.entity.GcpEntity;
import gcp.cloudblog_mailing.model.entity.GcpRelationship;
import gcp.cloudblog_mailing.repository.GcpEntityRepository;
import gcp.cloudblog_mailing.repository.GcpRelationshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GcpDocsIngestionService {

    private final VectorStore vectorStore;
    private final ChatModel chatModel;
    private final GcpEntityRepository entityRepository;
    private final GcpRelationshipRepository relationshipRepository;

    private static final String BLOG_RSS_URL = "https://cloudblog.withgoogle.com/rss/";
    private static final String RELEASE_NOTES_FEED_URL = "https://cloud.google.com/feeds/gcp-release-notes.xml";

    /**
     * Crawls both the Google Cloud Blog RSS and GCP Release Notes, chunks and vectorizes them,
     * and extracts Knowledge Graph triples.
     */
    @Transactional
    public void ingestLatestBlogPosts() throws IOException {
        log.info("Starting ingestion phase...");

        // 1. Pre-seed core GCP services first
        preSeedGcpArchitectureGraph();

        // 2. Ingest Google Cloud Blog
        try {
            ingestFeed(BLOG_RSS_URL, "Blog Post", "item", "title", "description", "link", "pubDate");
        } catch (Exception e) {
            log.error("Failed to ingest Google Cloud Blog feed: {}", e.getMessage());
        }

        // 3. Ingest GCP Release Notes (handles Atom-style feeds resiliently)
        try {
            ingestFeed(RELEASE_NOTES_FEED_URL, "Release Note", "entry", "title", "content", "link", "updated");
        } catch (Exception e) {
            log.error("Failed to ingest GCP Release Notes feed: {}", e.getMessage());
        }

        log.info("Successfully completed dual-source ingestion.");
    }

    /**
     * Helper method to parse, split, vectorize, and graph-extract XML feeds.
     * Supports both RSS (<item>) and Atom (<entry>) schemas.
     */
    private void ingestFeed(String url, String sourceType, String itemTag, String titleTag, 
                            String descTag, String linkTag, String dateTag) throws IOException {
        log.info("Ingesting {} feed from: {}", sourceType, url);
        
        Document doc = Jsoup.connect(url)
                .parser(Parser.xmlParser())
                .get();

        Elements items = doc.select(itemTag);
        TokenTextSplitter splitter = new TokenTextSplitter();

        for (Element item : items) {
            String title = item.select(titleTag).first() != null ? item.select(titleTag).first().text() : "No Title";
            String description = item.select(descTag).first() != null ? item.select(descTag).first().text() : "";
            
            // Jsoup Atom link handles differently (extracting href attribute)
            String link = "";
            Element linkElement = item.select(linkTag).first();
            if (linkElement != null) {
                link = linkElement.hasAttr("href") ? linkElement.attr("href") : linkElement.text();
            }

            String pubDateStr = item.select(dateTag).first() != null ? item.select(dateTag).first().text() : "";

            if (description.isEmpty() && item.select("summary").first() != null) {
                description = item.select("summary").first().text();
            }

            log.info("Processing {} item: {}", sourceType, title);

            // Create Spring AI Document with source_type metadata for cross-checking
            org.springframework.ai.document.Document aiDoc = new org.springframework.ai.document.Document(
                    "Source Type: " + sourceType + "\nTitle: " + title + "\n\nContent: " + description,
                    Map.of(
                            "title", title,
                            "link", link,
                            "pubDate", pubDateStr,
                            "source_type", sourceType,
                            "source", sourceType.equals("Blog Post") ? "Google Cloud Blog" : "GCP Release Notes"
                    )
            );

            // Chunk and store in PgVectorStore via Spring AI
            List<org.springframework.ai.document.Document> splitDocs = splitter.split(List.of(aiDoc));
            log.info("Vectorizing {} chunks for: {}", splitDocs.size(), title);
            vectorStore.accept(splitDocs);

            // Extract Graph Entities and Relationships using Vertex AI Gemini
            extractAndSaveTriples(title + "\n" + description);
        }
    }

    /**
     * Pre-seeds core GCP architecture relationships into the database so the Knowledge Graph
     * starts with high-fidelity professional definitions.
     */
    @Transactional
    public void preSeedGcpArchitectureGraph() {
        log.info("Pre-seeding core GCP Architecture graph...");

        // Define core entities
        Map<String, String[]> coreEntities = Map.of(
            "cloud_run", new String[]{"Cloud Run", "Service", "Fully managed serverless container runtime"},
            "cloud_functions", new String[]{"Cloud Functions", "Service", "Event-driven serverless function-as-a-service (FaaS)"},
            "gke", new String[]{"Google Kubernetes Engine (GKE)", "Service", "Secured and fully managed Kubernetes service"},
            "alloydb", new String[]{"AlloyDB for PostgreSQL", "Service", "Fully managed, PostgreSQL-compatible relational database service"},
            "cloud_spanner", new String[]{"Cloud Spanner", "Service", "Fully managed relational database with unlimited scale and strong consistency"},
            "serverless", new String[]{"Serverless Compute", "Concept", "Computing model with auto-scaling and zero-idle cost"},
            "containers", new String[]{"Containers", "Feature", "Standardized software packaging format"},
            "code_snippets", new String[]{"Code Snippets", "Feature", "Source code functions triggered by cloud events"}
        );

        for (var entry : coreEntities.entrySet()) {
            String id = entry.getKey();
            String[] details = entry.getValue();
            if (!entityRepository.existsById(id)) {
                entityRepository.save(GcpEntity.builder()
                        .entityId(id)
                        .name(details[0])
                        .entityType(details[1])
                        .description(details[2])
                        .build());
            }
        }

        // Define core relationships
        List<Object[]> relationships = List.of(
            new Object[]{"cloud_run", "serverless", "IS_A", "Cloud Run is a serverless runtime"},
            new Object[]{"cloud_functions", "serverless", "IS_A", "Cloud Functions is an event-driven serverless runtime"},
            new Object[]{"cloud_run", "containers", "RUNS", "Cloud Run packages and runs application containers"},
            new Object[]{"cloud_functions", "code_snippets", "RUNS", "Cloud Functions runs lightweight standalone code snippets"},
            new Object[]{"cloud_run", "cloud_functions", "COMPETITOR_OF", "Both are serverless runtimes. Cloud Run supports containers, while Cloud Functions is ideal for simple functions."},
            new Object[]{"alloydb", "cloud_spanner", "ALTERNATIVE_OF", "AlloyDB is for PostgreSQL scaling, Spanner is for global multi-region consistency"}
        );

        for (Object[] r : relationships) {
            String sourceId = (String) r[0];
            String targetId = (String) r[1];
            String type = (String) r[2];
            String desc = (String) r[3];

            GcpEntity source = entityRepository.findById(sourceId).orElse(null);
            GcpEntity target = entityRepository.findById(targetId).orElse(null);

            if (source != null && target != null) {
                boolean exists = relationshipRepository.findByEntityId(sourceId).stream()
                        .anyMatch(rel -> rel.getTargetEntity().getEntityId().equals(targetId) 
                                && rel.getRelationType().equals(type));
                
                if (!exists) {
                    relationshipRepository.save(GcpRelationship.builder()
                            .sourceEntity(source)
                            .targetEntity(target)
                            .relationType(type)
                            .description(desc)
                            .build());
                }
            }
        }
        log.info("Core GCP Architecture graph seeded.");
    }

    /**
     * Uses Gemini to extract entities and relations from a text block, saving them into the DB.
     */
    private void extractAndSaveTriples(String text) {
        String prompt = """
            Extract key Google Cloud services, tools, and technical concepts mentioned in the text below, along with their relationships.
            Format the output strictly as a JSON list of objects with the following fields:
            - source: name of the source entity (e.g. "Cloud Run")
            - source_type: type of source entity (must be one of: "Service", "Concept", "Feature")
            - target: name of the target entity (e.g. "Containers")
            - target_type: type of target entity (must be one of: "Service", "Concept", "Feature")
            - relationship: the relation verb in uppercase (e.g. "RUNS", "IS_A", "INTEGRATES_WITH", "COMPETITOR_OF", "ALTERNATIVE_OF")
            - description: brief description of how they are related.

            Example Output:
            [
               {"source": "Cloud Run", "source_type": "Service", "target": "Containers", "target_type": "Feature", "relationship": "RUNS", "description": "Cloud Run allows you to run containerized applications serverless."}
            ]

            Text to analyze:
            ---
            %s
            ---
            """;

        try {
            log.info("Requesting Gemini relationship extraction...");
            String jsonResponse = chatModel.call(String.format(prompt, text));
            
            // Strip markdown block fences if returned
            if (jsonResponse.contains("```json")) {
                jsonResponse = jsonResponse.substring(jsonResponse.indexOf("```json") + 7);
                jsonResponse = jsonResponse.substring(0, jsonResponse.indexOf("```"));
            } else if (jsonResponse.contains("```")) {
                jsonResponse = jsonResponse.substring(jsonResponse.indexOf("```") + 3);
                jsonResponse = jsonResponse.substring(0, jsonResponse.indexOf("```"));
            }
            
            jsonResponse = jsonResponse.trim();

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, String>> triples = mapper.readValue(jsonResponse, new com.fasterxml.jackson.core.type.TypeReference<>() {});

            for (Map<String, String> triple : triples) {
                String srcName = triple.get("source");
                String srcType = triple.get("source_type");
                String tgtName = triple.get("target");
                String tgtType = triple.get("target_type");
                String relType = triple.get("relationship");
                String desc = triple.get("description");

                if (srcName == null || tgtName == null || relType == null) continue;

                // Save or retrieve entities
                GcpEntity sourceEntity = getOrCreateEntity(srcName, srcType);
                GcpEntity targetEntity = getOrCreateEntity(tgtName, tgtType);

                // Save relationship
                boolean relExists = relationshipRepository.findByEntityId(sourceEntity.getEntityId()).stream()
                        .anyMatch(r -> r.getTargetEntity().getEntityId().equals(targetEntity.getEntityId()) 
                                && r.getRelationType().equalsIgnoreCase(relType));

                if (!relExists) {
                    relationshipRepository.save(GcpRelationship.builder()
                            .sourceEntity(sourceEntity)
                            .targetEntity(targetEntity)
                            .relationType(relType.toUpperCase())
                            .description(desc)
                            .build());
                    log.info("Saved extracted relationship: ({}) -[{}]-> ({})", srcName, relType, tgtName);
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract triples from text: {}", e.getMessage());
        }
    }

    private GcpEntity getOrCreateEntity(String name, String type) {
        String id = name.toLowerCase().replaceAll("[^a-zA-Z0-9]", "_");
        return entityRepository.findById(id).orElseGet(() -> 
            entityRepository.save(GcpEntity.builder()
                    .entityId(id)
                    .name(name)
                    .entityType(type != null ? type : "Service")
                    .description("Auto-extracted from RSS feed content.")
                    .build())
        );
    }
}
