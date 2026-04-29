package com.chongcc.test.Service.AI;

import com.chongcc.test.Entity.DialHst;
import com.chongcc.test.dao.DialHstRepo;
import com.chongcc.test.Result.ApiResponse;
import com.chongcc.test.dto.AiRequest;
import com.chongcc.test.dto.HstResponse;
import com.coze.openapi.client.chat.CreateChatReq;
import com.coze.openapi.client.chat.model.ChatEvent;
import com.coze.openapi.client.chat.model.ChatEventType;
import com.coze.openapi.client.connversations.message.model.Message;
import com.coze.openapi.service.auth.TokenAuth;
import com.coze.openapi.service.service.CozeAPI;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Flowable;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class AIPowerPointServiceImpl implements AIPowerPointService{
    private final String cozeApiToken;
    private final String cozeApiBotId;
    private final String cozeApiBaseUrl;
    private final DialHstRepo dialHstRepo;
    private static final String hstURL = "https://api.coze.cn/v3/chat/message/list";
    public AIPowerPointServiceImpl(
            @Value("${coze.api.token}") String cozeApiToken,
            @Value("${coze.api.botId-ppt}") String cozeApiBotId,
            @Value("${coze.api.baseUrl}") String cozeApiBaseUrl,
            DialHstRepo dialHstRepo) {
        this.cozeApiToken = cozeApiToken;
        this.cozeApiBotId = cozeApiBotId;
        this.cozeApiBaseUrl = cozeApiBaseUrl;
        this.dialHstRepo = dialHstRepo;
    }

    @Override
    public Flowable<ChatEvent> generatePowerPoint(AiRequest aiRequest) {
        System.out.println("generatePowerPoint" + aiRequest);
        TokenAuth authCli = new TokenAuth(cozeApiToken);
        CozeAPI coze = new CozeAPI.Builder()
                .baseURL(cozeApiBaseUrl)
                .auth(authCli)
                .readTimeout(600000)
                .build();

        CreateChatReq req = CreateChatReq.builder()
                .botID(cozeApiBotId)
                .userID(aiRequest.getUserId())
                .messages(Collections.singletonList(
                        Message.buildUserQuestionText(aiRequest.getContent())
                ))
                .build();
        AtomicBoolean firstEventHandled = new AtomicBoolean(false);

        return coze.chat().stream(req)
                .filter(chatEvent -> chatEvent.getEvent().equals(ChatEventType.CONVERSATION_MESSAGE_COMPLETED)
                        || chatEvent.getEvent().equals(ChatEventType.CONVERSATION_CHAT_COMPLETED))
//                .filter(chatEvent -> chatEvent.getMessage() != null)
                .doOnNext(chatEvent -> {
                    if (firstEventHandled.compareAndSet(false, true)) {
                        // 只对第一个匹配的事件执行副作用
                        DialHst dialHst = new DialHst();
                        String userIdStr = aiRequest.getUserId();
                        if (StringUtils.isNotBlank(userIdStr)) {
                            try {
                                dialHst.setUserId(Integer.parseInt(userIdStr));
                            } catch (NumberFormatException e) {
                                System.err.println("Invalid user ID format: " + userIdStr);
                            }
                        }
                        dialHst.setQuestion(aiRequest.getContent());
                        dialHst.setConversationId(chatEvent.getMessage().getConversationId());
                        dialHst.setChatId(chatEvent.getMessage().getChatId());
                        dialHst.setDialType("ppt");
                        dialHstRepo.save(dialHst);
                    }
                });
    }
    @Override
    public ApiResponse<List<HstResponse>> getHstByUserId(Integer userId) {
        if (userId == null) {
            return new ApiResponse<>(400, "Null userId", null);
        }
        try {
            List<DialHst> dialHsts = dialHstRepo.findByUserIdAndDialType(userId, "ppt");
            List<HstResponse> hstResponses = dialHsts.stream()
                    .map(dialHst -> new HstResponse(
                            dialHst.getId(),
                            dialHst.getDialType(),
                            dialHst.getQuestion(),
                            dialHst.getConversationId(),
                            dialHst.getChatId(),
                            null,
                            dialHst.getUserId()
                    ))
                    .toList();
            return new ApiResponse<>(200, "success", hstResponses);
        } catch (Exception e) {
            return new ApiResponse<>(500, "Failed to retrieve history: " + e.getMessage(), null);
        }
    }

    @Override
    public ApiResponse<?> getHstById(String conversationId, String chatId) {
        String url = hstURL + "?conversation_id=" + URLEncoder.encode(conversationId, StandardCharsets.UTF_8) +
                "&chat_id=" + URLEncoder.encode(chatId, StandardCharsets.UTF_8);
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + cozeApiToken)
                .header("Content-Type", "application/json")
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);

            List<Map<String, Object>> dataList = (List<Map<String, Object>>) result.get("data");

            if (dataList != null && !dataList.isEmpty()){
                return new ApiResponse<>(200, "success", dataList);
            }
            return new ApiResponse<>(404, "Not Found Related Data", null);
        } catch (Exception e){
            return new ApiResponse<>(500, "Failed to retrieve history: " + e.getMessage(), null);
        }
    }

    @Override
    public void deleteHst(Integer id) {
        dialHstRepo.deleteById(id);
    }
    @Override
    public HstResponse findById(Integer id) {
        DialHst dialHst = dialHstRepo.findById(id).orElseThrow(() -> new RuntimeException("DialHst is not exists"));
        return new HstResponse(
                dialHst.getId(),
                dialHst.getDialType(),
                dialHst.getQuestion(),
                dialHst.getConversationId(),
                dialHst.getChatId(),
                null,
                dialHst.getUserId());
    }
}
