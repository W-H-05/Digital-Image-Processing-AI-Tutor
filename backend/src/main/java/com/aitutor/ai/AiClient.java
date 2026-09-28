package com.aitutor.ai;

import com.aitutor.common.BizException;
import com.aitutor.config.AiProperties;
import com.aitutor.config.RuntimeConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AI HTTP 客户端：调用 DeepSeek 兼容 OpenAI 协议的 /chat/completions
 * 配置优先级：运行时配置(system_config 表) > .env 默认值，前端可动态修改。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiClient {

    private final AiProperties aiProperties;
    private final RuntimeConfigService configService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private String baseUrl() {
        return configService.get("ai.baseUrl", aiProperties.getBaseUrl());
    }

    private String apiKey() {
        return configService.get("ai.apiKey", aiProperties.getApiKey());
    }

    private String model() {
        return configService.get("ai.model", aiProperties.getModel());
    }

    private String visionModel() {
        return configService.get("ai.visionModel", aiProperties.getVisionModel());
    }

    /**
     * 文本对话（非流式，返回完整回答）
     */
    public String chat(List<Map<String, String>> messages, double temperature) {
        return request(model(), messages, null, temperature);
    }

    /**
     * 视觉对话：messages 中的 user content 可包含 image_url(base64 data url)
     */
    public String chatWithImage(String systemPrompt, String userText, String base64Image, String mimeType) {
        ObjectNode sysMsg = objectMapper.createObjectNode();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);

        ObjectNode userContent = objectMapper.createObjectNode();
        ArrayNode contentArr = objectMapper.createArrayNode();
        ObjectNode textPart = objectMapper.createObjectNode();
        textPart.put("type", "text");
        textPart.put("text", userText);
        contentArr.add(textPart);
        ObjectNode imagePart = objectMapper.createObjectNode();
        imagePart.put("type", "image_url");
        ObjectNode imageUrl = objectMapper.createObjectNode();
        imageUrl.put("url", "data:" + mimeType + ";base64," + base64Image);
        imagePart.set("image_url", imageUrl);
        contentArr.add(imagePart);
        userContent.put("role", "user");
        userContent.set("content", contentArr);

        List<ObjectNode> msgs = List.of(sysMsg, userContent);
        return request(visionModel(), msgs, contentArr, 0.3);
    }

    private String request(String model, List<?> messages, Object fallback, double temperature) {
        String endpoint = trimTrailing(baseUrl()) + "/chat/completions";
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("max_tokens", 2048);
        body.put("stream", false);
        ArrayNode arr = objectMapper.createArrayNode();
        messages.forEach(arr::addPOJO);
        body.set("messages", arr);

        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("AI 调用失败 status={} body={}", response.statusCode(), response.body());
                throw new BizException(502, "AI 服务调用失败（状态码 " + response.statusCode() + "），请检查 API Key 配置");
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode content = choices.get(0).path("message").path("content");
                return content.asText("");
            }
            return "";
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 请求异常", e);
            throw new BizException(502, "AI 服务连接失败，请检查网络与 API 配置");
        }
    }

    private String trimTrailing(String url) {
        if (url == null) return "";
        String s = url.trim();
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }
}
