package com.chongcc.test.Controller.AI;

import java.util.List;
import java.util.Map;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.chongcc.test.Service.AI.AIMultiModalService;
import io.reactivex.schedulers.Schedulers;

@RestController
@RequestMapping("/score")
public class ScoreController {
    private final AIMultiModalService aiMultiModalService;
    public ScoreController(AIMultiModalService aiMultiModalService) {
        this.aiMultiModalService = aiMultiModalService;
    }

    @PostMapping(value = "/score")
    public SseEmitter scoreImg(@RequestBody Map<String, String> requestBody) {
        String imageBase64 = requestBody.get("imageBase64");
        SseEmitter emitter = new SseEmitter(0L);
        aiMultiModalService.streamMultiModalChatWithBase64(imageBase64)
                .subscribeOn(Schedulers.io())
                .subscribe(
                        message -> {
                            try {
                                // 推理内容
                                String reasoning = message.getOutput().getChoices().get(0).getMessage().getReasoningContent();
                                if (reasoning != null && !reasoning.isEmpty()) {
                                    emitter.send(SseEmitter.event().name("reasoning").data(reasoning));
                                }
                                // 最终内容
                                List<Map<String, Object>> contentList = message.getOutput().getChoices().get(0).getMessage().getContent();
                                if (contentList != null && !contentList.isEmpty()) {
                                    Object text = contentList.get(0).get("text");
                                    if (text != null) {
                                        emitter.send(SseEmitter.event().name("content").data(text));
                                    }
                                }
                            } catch (Exception e) {
                                try { emitter.completeWithError(e); } catch (Exception ignore) {}
                            }
                        },
                        error -> {
                            try { emitter.send(SseEmitter.event().name("error").data(error.getMessage())); emitter.completeWithError(error); } catch (Exception ignore) {}
                        },
                        () -> {
                            try { emitter.send(SseEmitter.event().name("complete").data("Stream completed")); emitter.complete(); } catch (Exception ignore) {}
                        }
                );
        return emitter;
    }
}
