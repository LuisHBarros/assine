package br.com.assine.content.adapter.out.notion;

import br.com.assine.content.domain.model.NewsletterContent;
import br.com.assine.content.domain.port.out.ContentSourcePort;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class NotionAdapter implements ContentSourcePort {

    private static final Logger log = LoggerFactory.getLogger(NotionAdapter.class);

    private final WebClient webClient;
    private final String databaseId;

    public NotionAdapter(
            WebClient.Builder webClientBuilder,
            @Value("${notion.api.key}") String apiKey,
            @Value("${notion.database.id}") String databaseId) {
        this.databaseId = databaseId;
        this.webClient = webClientBuilder
                .baseUrl("https://api.notion.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Notion-Version", "2022-06-28")
                .build();
    }

    @Override
    public Optional<NewsletterContent> fetchTodayContent() {
        LocalDate today = LocalDate.now();
        log.info("Fetching content from Notion for date: {}", today);

        try {
            // 1. Query database for page with Date=Today and Status=Pronto
            String queryJson = """
                {
                    "filter": {
                        "and": [
                            {
                                "property": "Date",
                                "date": {
                                    "equals": "%s"
                                }
                            },
                            {
                                "property": "Status",
                                "select": {
                                    "equals": "Pronto"
                                }
                            }
                        ]
                    }
                }
                """.formatted(today);

            JsonNode response = webClient.post()
                    .uri("/databases/{id}/query", databaseId)
                    .bodyValue(queryJson)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> 
                        clientResponse.bodyToMono(String.class)
                            .flatMap(error -> Mono.error(new RuntimeException("Notion API error: " + error)))
                    )
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || !response.has("results") || response.get("results").isEmpty()) {
                log.warn("No pages found in Notion for today with Status=Pronto");
                return Optional.empty();
            }

            JsonNode page = response.get("results").get(0);
            String pageId = page.get("id").asText();
            String title = extractTitle(page);

            log.info("Found page in Notion: {} (ID: {})", title, pageId);

            // 2. Fetch blocks for the page
            String bodyHtml = fetchBlocksAsHtml(pageId);

            return Optional.of(new NewsletterContent(title, bodyHtml, today));

        } catch (Exception e) {
            log.error("Failed to fetch content from Notion", e);
            return Optional.empty();
        }
    }

    private String extractTitle(JsonNode page) {
        try {
            JsonNode properties = page.get("properties");
            JsonNode titleProp = properties.get("Title"); // Assuming property name is "Title"
            if (titleProp == null) titleProp = properties.get("Name"); // Fallback
            
            return titleProp.get("title").get(0).get("plain_text").asText();
        } catch (Exception e) {
            return "Sem Título";
        }
    }

    private String fetchBlocksAsHtml(String pageId) {
        try {
            JsonNode response = webClient.get()
                    .uri("/blocks/{id}/children", pageId)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || !response.has("results")) {
                return "";
            }

            return StreamSupport.stream(response.get("results").spliterator(), false)
                    .map(this::blockToHtml)
                    .collect(Collectors.joining("\n"));

        } catch (Exception e) {
            log.error("Failed to fetch blocks for page {}", pageId, e);
            return "Erro ao carregar conteúdo.";
        }
    }

    private String blockToHtml(JsonNode block) {
        String type = block.get("type").asText();
        JsonNode content = block.get(type);

        return switch (type) {
            case "paragraph" -> wrap("p", extractPlainText(content));
            case "heading_1" -> wrap("h1", extractPlainText(content));
            case "heading_2" -> wrap("h2", extractPlainText(content));
            case "bulleted_list_item" -> wrap("li", extractPlainText(content)); // Should wrap in <ul> but for simplicity in email...
            case "numbered_list_item" -> wrap("li", extractPlainText(content)); // Should wrap in <ol>
            default -> "";
        };
    }

    private String extractPlainText(JsonNode content) {
        try {
            JsonNode richText = content.get("rich_text");
            return StreamSupport.stream(richText.spliterator(), false)
                    .map(node -> node.get("plain_text").asText())
                    .collect(Collectors.joining());
        } catch (Exception e) {
            return "";
        }
    }

    private String wrap(String tag, String text) {
        if (text == null || text.isBlank()) return "";
        return "<" + tag + ">" + text + "</" + tag + ">";
    }
}
