package com.rentpro.backend.ai;

import com.rentpro.backend.security.JwtUserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    private final RestClient restClient = RestClient.create();
    private final AiContextService aiContextService;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    public AiChatController(AiContextService aiContextService) {
        this.aiContextService = aiContextService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(Authentication auth,
                                              @Valid @RequestBody ChatRequest req) {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            return ResponseEntity.status(503)
                    .body(new ChatResponse("AI assistant is not configured. Please set GROQ_API_KEY."));
        }

        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID userId = UUID.fromString(ctx.userId());
        String role = ctx.role();

        String dataContext = "OWNER".equals(role)
                ? aiContextService.buildOwnerContext(userId)
                : aiContextService.buildTenantContext(userId);

        String systemPrompt = buildSystemPrompt(role, dataContext);

        String safeUserMessage = "[USER_MESSAGE_START]\n" + req.message() + "\n[USER_MESSAGE_END]";

        Map<String, Object> body = Map.of(
                "model", MODEL,
                "max_tokens", 1024,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", safeUserMessage)
                )
        );

        GroqResponse groqResponse = restClient.post()
                .uri(GROQ_URL)
                .header("Authorization", "Bearer " + groqApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(GroqResponse.class);

        String reply = (groqResponse != null && !groqResponse.choices().isEmpty())
                ? groqResponse.choices().get(0).message().content()
                : "I couldn't generate a response. Please try again.";

        return ResponseEntity.ok(new ChatResponse(reply));
    }

    private String buildSystemPrompt(String role, String dataContext) {
        String intro = "OWNER".equals(role)
                ? "You are RentPro Assistant for a property owner/landlord. Answer questions using ONLY the real data provided below — never invent or guess figures. Each payment shows its own currency."
                : "You are RentPro Assistant for a tenant. Answer questions using ONLY the real data provided below — never invent or guess figures. Each payment shows its own currency.";
        return intro + "\n\n" + dataContext;
    }

    public record ChatRequest(
            @NotBlank @Size(max = 2000) String message
    ) {}

    public record ChatResponse(String reply) {}

    private record GroqResponse(List<Choice> choices) {
        private record Choice(Message message) {}
        private record Message(String role, String content) {}
    }
}
