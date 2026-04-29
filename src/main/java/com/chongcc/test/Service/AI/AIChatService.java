package com.chongcc.test.Service.AI;

import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AIChatService {
    /**
     * 流式聊天接口
     * @param messages 聊天消息列表
     * @return 返回Flux流式响应
     */
    Flux<String> streamChat(List<ChatMessage> messages);
}
