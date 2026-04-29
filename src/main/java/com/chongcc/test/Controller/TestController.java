package com.chongcc.test.Controller;

import org.springframework.boot.SpringApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RestController
public class TestController {
    private static final String AI_SERVICE_URL = "http://10.10.5.59/ds7b/v1/chat/completions";
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/testai")
    public SseEmitter askQuestion(@RequestBody QuestionRequest request) {
        SseEmitter emitter = new SseEmitter(30_000L); // 30秒超时

        // 构建请求体
        AiServiceRequest aiRequest = new AiServiceRequest();
        aiRequest.setModel("DeepSeek-R1-Distill-Qwen-7B");
        aiRequest.setStream(false);
        aiRequest.setTemperature(0.7);
        aiRequest.setMaxTokens(1000);

        Message message = new Message();
        message.setRole("user");
        message.setContent(request.getQuestion());
        aiRequest.setMessages(new Message[]{message});

        try {
            String jsonBody = objectMapper.writeValueAsString(aiRequest);

            Request httpRequest = new Request.Builder()
                    .url(AI_SERVICE_URL)
                    .post(okhttp3.RequestBody.create(jsonBody, okhttp3.MediaType.parse(MediaType.APPLICATION_JSON_VALUE)))
                    .build();

            client.newCall(httpRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    emitter.completeWithError(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        emitter.completeWithError(new IOException("Unexpected code " + response));
                        return;
                    }

                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody != null) {
                            String responseText = responseBody.string();
                            System.out.println(responseText);
                            
                            try {
                                AiResponse aiResponse = objectMapper.readValue(responseText, AiResponse.class);
                                if (aiResponse.getChoices() != null && aiResponse.getChoices().length > 0) {
                                    String content = aiResponse.getChoices()[0].getMessage().getContent();
                                    if (content != null && !content.isEmpty()) {
                                        // 发送完整答案
                                        emitter.send(SseEmitter.event()
                                                .data(content)
                                                .name("complete"));
                                        emitter.complete();
                                    } else {
                                        emitter.completeWithError(new IOException("Empty response content"));
                                    }
                                } else {
                                    emitter.completeWithError(new IOException("No choices in response"));
                                }
                            } catch (Exception e) {
                                emitter.completeWithError(new IOException("Failed to parse response: " + e.getMessage()));
                            }
                        }
                    }
                }
            });

        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    // 异常处理
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        ErrorResponse error = new ErrorResponse(e.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static void main(String[] args) {
        SpringApplication.run(TestController.class, args);
    }

    // 请求和响应类
    static class QuestionRequest {
        private String question;

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }
    }

    static class AiServiceRequest {
        private Message[] messages;
        private String model;
        private boolean stream;
        private double temperature;
        private int maxTokens;

        // getters and setters
        public Message[] getMessages() { return messages; }
        public void setMessages(Message[] messages) { this.messages = messages; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public boolean isStream() { return stream; }
        public void setStream(boolean stream) { this.stream = stream; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    }

    static class Message {
        private String role;
        private String content;

        // getters and setters
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    static class AiResponse {
        private String id;
        private String object;
        private long created;
        private String model;
        private Choice[] choices;
        private Usage usage;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getObject() { return object; }
        public void setObject(String object) { this.object = object; }
        
        public long getCreated() { return created; }
        public void setCreated(long created) { this.created = created; }
        
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        
        public Choice[] getChoices() { return choices; }
        public void setChoices(Choice[] choices) { this.choices = choices; }
        
        public Usage getUsage() { return usage; }
        public void setUsage(Usage usage) { this.usage = usage; }
    }

    static class Choice {
        private int index;
        private Message message;
        @JsonProperty("finish_reason")
        private String finishReason;

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }
        
        public Message getMessage() { return message; }
        public void setMessage(Message message) { this.message = message; }
        
        public String getFinishReason() { return finishReason; }
        public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
    }

    static class Usage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;
        @JsonProperty("total_tokens")
        private int totalTokens;
        @JsonProperty("completion_tokens")
        private int completionTokens;

        public int getPromptTokens() { return promptTokens; }
        public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }
        
        public int getTotalTokens() { return totalTokens; }
        public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }
        
        public int getCompletionTokens() { return completionTokens; }
        public void setCompletionTokens(int completionTokens) { this.completionTokens = completionTokens; }
    }

    static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
