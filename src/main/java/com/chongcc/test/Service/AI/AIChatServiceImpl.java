package com.chongcc.test.Service.AI;

import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.service.ArkService;
import okhttp3.ConnectionPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

import javax.annotation.PreDestroy;
import java.util.List;

import java.util.concurrent.TimeUnit;

@Service
public class AIChatServiceImpl implements AIChatService {
    private final String apiKey;
    private final String model;
    public AIChatServiceImpl(@Value("${volc.api-key}") String apiKey,
                             @Value("${volc.chat-model}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }
    // 使用连接池提高性能
    private final ConnectionPool connectionPool = new ConnectionPool(10, 5, TimeUnit.MINUTES);

    @Override
    public Flux<String> streamChat(List<ChatMessage> messages) {
        return Flux.create(emitter -> {
            ArkService service = ArkService.builder()
                    .apiKey(apiKey)
                    .connectionPool(connectionPool)
                    .build();

            try {
                ChatCompletionRequest request = ChatCompletionRequest.builder()
                        .model(model)
                        .messages(messages)
                        .stream(true)
                        .build();

                service.streamChatCompletion(request)
                        .doOnError(emitter::error)
                        .doOnComplete(emitter::complete)
                        .subscribe(chunk -> {
                            if (chunk.getChoices() != null) {
                                chunk.getChoices().forEach(choice -> {
                                    if (choice.getMessage() != null
                                            && choice.getMessage().getContent() != null) {
                                        emitter.next((String) choice.getMessage().getContent());
                                    }
                                });
                            }
                        });

                // 清理钩子
                emitter.onDispose(() -> {
                    service.shutdownExecutor();
                });
            } catch (Exception e) {
                emitter.error(e);
            }
        });
    }
}
