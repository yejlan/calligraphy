package com.chongcc.test.Service.AI;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import io.reactivex.Flowable;

public interface AIMultiModalService {
    /**
     * 多模态流式对话接口（单图片base64+固定提示词）
     * @param imageBase64 图片base64字符串（带data:image/jpeg;base64,前缀或不带均可）
     * @return Flowable流式响应，原始AI事件
     */
    Flowable<MultiModalConversationResult> streamMultiModalChatWithBase64(String imageBase64);
}
