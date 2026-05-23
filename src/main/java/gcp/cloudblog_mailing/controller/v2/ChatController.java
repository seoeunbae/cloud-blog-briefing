package gcp.cloudblog_mailing.controller.v2;

import gcp.cloudblog_mailing.ingestion.GcpDocsIngestionService;
import gcp.cloudblog_mailing.model.entity.GcpEntity;
import gcp.cloudblog_mailing.model.entity.GcpRelationship;
import gcp.cloudblog_mailing.repository.GcpEntityRepository;
import gcp.cloudblog_mailing.repository.GcpRelationshipRepository;
import gcp.cloudblog_mailing.rag.GcpDocChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/v2")
@RequiredArgsConstructor
public class ChatController {

    private final GcpDocChatService chatService;
    private final GcpDocsIngestionService ingestionService;
    private final GcpEntityRepository entityRepository;
    private final GcpRelationshipRepository relationshipRepository;

    /**
     * Renders the premium interactive chat visualizer interface.
     */
    @GetMapping("/chat")
    public String getChatInterface() {
        return "chat"; // Corresponds to templates/chat.html
    }

    /**
     * API to process CE natural language query and return hybrid RAG responses.
     */
    @PostMapping("/api/chat")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
        }
        try {
            Map<String, Object> response = chatService.answerQuestion(message);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during chat processing: ", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * API to manually trigger crawl and ingestion of RSS data + KG creation.
     */
    @PostMapping("/api/ingest")
    @ResponseBody
    public ResponseEntity<Map<String, String>> ingest() {
        try {
            ingestionService.ingestLatestBlogPosts();
            return ResponseEntity.ok(Map.of("message", "Ingestion completed successfully and core graph seeded."));
        } catch (Exception e) {
            log.error("Error during manual ingestion: ", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * API to return the entire active Knowledge Graph network (nodes and edges)
     * for interactive client-side network charting.
     */
    @GetMapping("/api/graph")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCompleteGraph() {
        try {
            List<GcpEntity> entities = entityRepository.findAll();
            List<GcpRelationship> relationships = relationshipRepository.findAll();

            List<Map<String, String>> nodes = entities.stream()
                    .map(e -> Map.of(
                            "id", e.getEntityId(),
                            "label", e.getName(),
                            "group", e.getEntityType(),
                            "title", e.getDescription() != null ? e.getDescription() : ""
                    ))
                    .collect(Collectors.toList());

            List<Map<String, String>> edges = relationships.stream()
                    .map(r -> Map.of(
                            "from", r.getSourceEntity().getEntityId(),
                            "to", r.getTargetEntity().getEntityId(),
                            "label", r.getRelationType(),
                            "title", r.getDescription() != null ? r.getDescription() : ""
                    ))
                    .collect(Collectors.toList());

            Map<String, Object> graphData = new HashMap<>();
            graphData.put("nodes", nodes);
            graphData.put("edges", edges);

            return ResponseEntity.ok(graphData);
        } catch (Exception e) {
            log.error("Error retrieving graph: ", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
