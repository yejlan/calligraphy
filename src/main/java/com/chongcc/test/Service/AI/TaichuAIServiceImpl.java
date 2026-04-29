package com.chongcc.test.Service.AI;

import com.chongcc.test.dto.AiServiceRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Service
public class TaichuAIServiceImpl implements TaichuAIService{
    private static final Logger log = LoggerFactory.getLogger(TaichuAIServiceImpl.class);
    private final String url;
    private final String llm;
    private final double temperature;
    private final int maxTokens;
    private final String prompt;
    public TaichuAIServiceImpl(@Value("${taichuai.url}") String url,
                               @Value("${taichuai.llm.model}") String llm,
                               @Value("${taichuai.llm.temperature}") double temperature,
                               @Value("${taichuai.llm.max_tokens}") int maxTokens,
                               @Value("${taichuai.llm.prompt}") String prompt){
        this.url = url;
        this.llm = llm;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.prompt = prompt;
    }
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public Flux<String> streamChat(String messages) {
        return Flux.create(emitter -> {
            AiServiceRequest request = new AiServiceRequest.RequestBuilder(llm, temperature, maxTokens)
                    .stream(true)
                    .message("user", prompt + messages)
                    .build();
            try {
                String jsonBody = objectMapper.writeValueAsString(request);
                Request httpRequest = new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(jsonBody, MediaType.parse(org.springframework.http.MediaType.APPLICATION_JSON_VALUE)))
                        .build();
                client.newCall(httpRequest).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        emitter.error(e);
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        if(!response.isSuccessful()){
                            emitter.error(new IOException("Unexpected code " + response));
                            return;
                        }
                        try (ResponseBody responseBody = response.body()) {
                            if (responseBody != null){
                                processStreamResponse(responseBody, emitter);
                            }
                        }
                    }
                });
            } catch (Exception e) {
                log.error("TaichuAIServiceErr", e);
                emitter.error(e);
            }
        });
    }
    private void processStreamResponse(ResponseBody responseBody, FluxSink<String> emitter) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody.byteStream()))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ") && !line.equals("data: [DONE]")) {
                    String jsonData = line.substring(6).trim(); // 去掉 "data: " 前缀并去除空格

                    if (jsonData.isEmpty()) {
                        continue; // 跳过空行
                    }

                    try {
                        AiStreamResponse streamResponse = objectMapper.readValue(jsonData, AiStreamResponse.class);

                        if (streamResponse.getChoices() != null && streamResponse.getChoices().length > 0) {
                            Choice choice = streamResponse.getChoices()[0];

                            if (choice.getDelta() != null) {
                                String deltaContent = choice.getDelta().getContent();

                                // 处理 role 字段（第一条消息通常是 role: "assistant"）
                                String role = choice.getDelta().getRole();
                                if (role != null && "assistant".equals(role)) {
                                    // 这是第一条消息，包含角色信息，通常没有内容
                                    // 可以忽略或者做特殊处理
                                    continue;
                                }

                                // 发送非空内容
                                if (deltaContent != null && !deltaContent.isEmpty()) {
                                    emitter.next(deltaContent);
                                }
                            }

                            // 检查是否结束
                            if (choice.getFinishReason() != null) {
                                if ("stop".equals(choice.getFinishReason())) {
                                    emitter.complete();
                                    break;
                                } else if ("length".equals(choice.getFinishReason())) {
                                    emitter.next("\n\n[达到长度限制]");
                                    emitter.complete();
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to parse JSON: " + jsonData);
                        System.err.println("Error: " + e.getMessage());
                        // 继续处理下一行
                    }
                }
            }
        } catch (IOException e) {
            emitter.error(e);
        }
    }
    @Getter
    static class AiStreamResponse {
        private String id;
        private String object;
        private long created;
        private String model;
        private Choice[] choices;
        private Usage usage;

    }
    @Getter
    static class Choice {
        private int index;
        private Delta delta;
        @JsonProperty("finish_reason")
        private String finishReason;
    }
    @Getter
    static class Usage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;
        @JsonProperty("total_tokens")
        private int totalTokens;
        @JsonProperty("completion_tokens")
        private int completionTokens;
    }
    @Getter
    static class Delta {
        private String role;
        private String content;
    }
}
