package com.chongcc.test.Controller.AI;

import com.chongcc.test.Service.AI.AIChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final AIChatService aiChatService;
    public ChatController(AIChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @GetMapping("/stream")
    public SseEmitter streamChat(@RequestParam String message) {
        SseEmitter emitter = new SseEmitter(6000_00L);

        // 1. 初始化对话上下文
        List<ChatMessage> messages = List.of(
                ChatMessage.builder().role(ChatMessageRole.SYSTEM).content("请你扮演一个对书法精通的角色").build(),
                ChatMessage.builder().role(ChatMessageRole.USER).content(message).build()
        );

        aiChatService.streamChat(messages)
                .subscribe(
                        content -> {
                            try {
                                Map<String, String> data = Map.of(
                                        "content", content,
                                        "role", "assistant"
                                );
                                String json = new ObjectMapper().writeValueAsString(data);

                                emitter.send(SseEmitter.event()
                                        .name("message")
                                        .data(json)
                                        .id(UUID.randomUUID().toString())
                                );
                            } catch (IOException e) {
                                System.err.println("Send error:" + e);
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            System.err.println("Stream error:" + error);
                            emitter.completeWithError(error);
                        },
                        emitter::complete
                );
        return emitter;
    }
}
