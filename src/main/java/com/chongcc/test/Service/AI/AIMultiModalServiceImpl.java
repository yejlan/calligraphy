package com.chongcc.test.Service.AI;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class AIMultiModalServiceImpl implements AIMultiModalService {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    @Value("${spring.ai.dashscope.model}")
    private String model;

    @Override
    public Flowable<MultiModalConversationResult> streamMultiModalChatWithBase64(String imageBase64) {
        try {
            MultiModalConversation conv = new MultiModalConversation();
            String prompt = "为书法作品评分";
            List<Map<String, Object>> content = new ArrayList<>();
            if (imageBase64 != null && imageBase64.startsWith("data:image")) {
                content.add(Collections.singletonMap("image", imageBase64));
            } else {
                content.add(Collections.singletonMap("image", "data:image/jpeg;base64," + imageBase64));
            }
            content.add(Collections.singletonMap("text", prompt));
            MultiModalMessage userMsg = MultiModalMessage.builder()
                    .role(Role.USER.getValue())
                    .content(content)
                    .build();
            MultiModalConversationParam param = buildMultiModalConversationParam(userMsg);
            return conv.streamCall(param);
        } catch (Exception e) {
            log.error("Error in multi-modal chat stream: {}", e.getMessage());
            return Flowable.error(e);
        }
    }

    private MultiModalConversationParam buildMultiModalConversationParam(MultiModalMessage msg) {
        String apiKeyValue = apiKey.isEmpty() ? System.getenv("DASHSCOPE_API_KEY") : apiKey;
        return MultiModalConversationParam.builder()
                .apiKey(apiKeyValue)
                .model(model)
                .messages(Arrays.asList(msg))
                .incrementalOutput(true)
                .build();
    }
}
