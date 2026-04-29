package com.chongcc.test.Controller.AI;

import com.chongcc.test.Result.ApiResponse;
import com.chongcc.test.Service.AI.AIPowerPointService;
import com.chongcc.test.dto.AiRequest;
import com.chongcc.test.dto.HstResponse;
import com.coze.openapi.client.chat.model.ChatEventType;
import io.reactivex.schedulers.Schedulers;
import org.springframework.cglib.proxy.Proxy;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RequestMapping("/coze")
@RestController
public class PowerPointController {
    private final AIPowerPointService aiPowerPointService;

    public PowerPointController(AIPowerPointService aiPowerPointService) {
        this.aiPowerPointService = aiPowerPointService;
    }

    @PostMapping("/powerpoint")
    public SseEmitter streamCompletedStatus(@RequestBody AiRequest request) throws Exception {
        SseEmitter emitter = new SseEmitter(3000_00L); // 设置超时时间 5 分钟

        aiPowerPointService.generatePowerPoint(request)
                .subscribeOn(Schedulers.io())
                .doOnNext(event -> {
                    try {
                        emitter.send(event);
                        if (event.getEvent().equals(ChatEventType.CONVERSATION_CHAT_COMPLETED)){
                            emitter.send(event);
                            emitter.complete();
                        }
                    } catch (Exception e) {
                        try {
                            emitter.completeWithError(e);
                        } catch (Exception ex) {
                            System.out.println("Error sending error event: " + ex.getMessage());
                        }
                    }
                })
                .doOnError(e -> {
                    try {
                        emitter.completeWithError(e);
                    } catch (Exception ex) {
                        // 日志记录或忽略
                    }
                }) // 错误结束
                .subscribe();

        return emitter;
    }
    @GetMapping("/powerpoint/history")
    public ApiResponse<List<HstResponse>> getHstByUserId(@RequestParam Integer userId) {
        return aiPowerPointService.getHstByUserId(userId);
    }
    @GetMapping("/powerpoint/history/detail")
    public ApiResponse<?> getHstById(@RequestParam String conversationId, @RequestParam String chatId) {
        return aiPowerPointService.getHstById(conversationId, chatId);
    }
    @DeleteMapping("/powerpoint/history")
    public ApiResponse<?> deleteHst(@RequestParam Integer id) {
        try {
            aiPowerPointService.deleteHst(id);
            return new ApiResponse<>(200, "delete hst success", null);
        } catch (Exception e) {
            return new ApiResponse<>(500, "delete hst failed", null);
        }
    }

}
