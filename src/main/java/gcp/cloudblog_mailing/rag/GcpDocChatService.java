package gcp.cloudblog_mailing.rag;

import gcp.cloudblog_mailing.model.entity.GcpEntity;
import gcp.cloudblog_mailing.model.entity.GcpRelationship;
import gcp.cloudblog_mailing.repository.GcpEntityRepository;
import gcp.cloudblog_mailing.repository.GcpRelationshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GcpDocChatService {

    private final VectorStore vectorStore;
    private final ChatModel chatModel;
    private final GcpEntityRepository entityRepository;
    private final GcpRelationshipRepository relationshipRepository;

    /**
     * Executes a hybrid search across relational Knowledge Graph and Vector Database
     * to answer the user's natural language question.
     */
    public Map<String, Object> answerQuestion(String query) {
        log.info("Processing RAG query: {}", query);

        // 1. Identify relevant Knowledge Graph entities by scanning the user query
        List<GcpEntity> allEntities = entityRepository.findAll();
        List<GcpEntity> matchedEntities = allEntities.stream()
                .filter(entity -> query.toLowerCase().contains(entity.getName().toLowerCase()))
                .collect(Collectors.toList());

        log.info("Matched graph entities in prompt: {}", matchedEntities.stream().map(GcpEntity::getName).collect(Collectors.toList()));

        // 2. Fetch all relationships involving the matched entities to form the Knowledge Graph Context
        List<GcpRelationship> matchedRelationships = new ArrayList<>();
        Set<Integer> processedRelIds = new HashSet<>();
        
        for (GcpEntity entity : matchedEntities) {
            List<GcpRelationship> rels = relationshipRepository.findByEntityId(entity.getEntityId());
            for (GcpRelationship r : rels) {
                if (processedRelIds.add(r.getRelationshipId())) {
                    matchedRelationships.add(r);
                }
            }
        }

        // Format Knowledge Graph context
        String graphContext = matchedRelationships.isEmpty() 
                ? "No direct Knowledge Graph relationships found for this query." 
                : matchedRelationships.stream()
                        .map(r -> String.format("- (%s) -[%s]-> (%s): %s",
                                r.getSourceEntity().getName(),
                                r.getRelationType(),
                                r.getTargetEntity().getName(),
                                r.getDescription()))
                        .collect(Collectors.joining("\n"));

        log.info("Extracted Knowledge Graph relations:\n{}", graphContext);

        // 3. Execute Semantic Similarity Search on AlloyDB Vector Store
        log.info("Searching Vector DB...");
        List<Document> vectorResults = vectorStore.similaritySearch(
                SearchRequest.query(query)
                        .withTopK(4)
                        .withSimilarityThreshold(0.5)
        );

        String vectorContext = vectorResults.isEmpty()
                ? "No relevant document chunks found in Vector Store."
                : vectorResults.stream()
                        .map(doc -> String.format("[Source: %s]\n%s", 
                                doc.getMetadata().getOrDefault("title", "GCP Doc"), 
                                doc.getContent()))
                        .collect(Collectors.joining("\n\n---\n\n"));

        log.info("Retrieved {} semantic chunks from Vector Store.", vectorResults.size());

        // 4. Create Structured RAG Prompt
        String systemPrompt = """
            You are Antigravity-RAG, a premium GCP Cloud Architecture RAG Chatbot designed to assist GCP Customer Engineers (CEs).
            Your goal is to answer technical questions with absolute accuracy, using both structured Knowledge Graph facts and unstructured Vector search document chunks.
            
            When answering comparative questions (such as "What is the difference between Cloud Run and Cloud Functions?"), synthesize the structured relationships to provide an authoritative comparison.
            
            Use the following context streams to answer the user's question. If the contexts do not contain enough information to answer, explain what is missing.
            
            === KNOWLEDGE GRAPH RELATIONSHIPS ===
            %s
            
            === SEMANTIC DOCUMENT CHUNKS ===
            %s
            
            Please formulate a professional, friendly, and structured response in Korean (한국어로 답변해 주세요). Use clear headers, tables for comparisons, and bullet points. Highlight differences in terms of:
            1. Packaging/Runtimes (Containers vs Code snippets)
            2. Scaling & Sizing Behavior
            3. Pricing & Idle Cost models
            4. Ideal Use cases
            """.formatted(graphContext, vectorContext);

        // 5. Call Gemini Chat Model via Spring AI
        log.info("Sending enriched prompt to Gemini...");
        String answer = chatModel.call(systemPrompt + "\n\nUser Question: " + query);

        // Return the final answer along with references for visualization
        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer);
        result.put("matchedEntities", matchedEntities);
        result.put("matchedRelationships", matchedRelationships.stream()
                .map(r -> Map.of(
                        "source", r.getSourceEntity().getName(),
                        "target", r.getTargetEntity().getName(),
                        "relationship", r.getRelationType(),
                        "description", r.getDescription() != null ? r.getDescription() : ""
                ))
                .collect(Collectors.toList()));

        // Format unique structured citations for cross-checking
        List<Map<String, Object>> citations = vectorResults.stream()
                .map(doc -> Map.of(
                        "title", doc.getMetadata().getOrDefault("title", "GCP Doc"),
                        "link", doc.getMetadata().getOrDefault("link", "#"),
                        "sourceType", doc.getMetadata().getOrDefault("source_type", "General Doc")
                ))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                m -> m.get("title").toString(),
                                m -> m,
                                (existing, replacement) -> existing
                        ),
                        m -> new ArrayList<>(m.values())
                ));
        result.put("citations", citations);

        return result;
    }
}
