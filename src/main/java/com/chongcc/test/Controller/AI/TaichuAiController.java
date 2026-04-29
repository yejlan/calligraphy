package com.chongcc.test.Controller.AI;

import com.chongcc.test.Service.AI.TaichuAIService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/taichuai")
public class TaichuAiController {
    private final TaichuAIService taichuAIService;
    public TaichuAiController(TaichuAIService taichuAIService) {
        this.taichuAIService = taichuAIService;
    }
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askQuestion(@RequestBody QuestionRequest request) {
        SseEmitter emitter = new SseEmitter(30_000L);

        // 调用服务层获取Flux
        Flux<String> responseFlux = taichuAIService.streamChat(request.getQuestion());

        // 将Flux转换为SSE
        responseFlux.subscribe(
                chunk -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .data(chunk)
                                .name("message"));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                error -> emitter.completeWithError(error),
                () -> emitter.complete()
        );

        // 处理SSE连接断开
        emitter.onCompletion(() -> System.out.println("SSE completed"));
        emitter.onTimeout(() -> System.out.println("SSE timed out"));
        emitter.onError((e) -> System.out.println("SSE error: " + e.getMessage()));

        return emitter;
    }
    static class QuestionRequest {
        private String question;

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }
    }
}
