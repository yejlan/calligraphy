package com.chongcc.test.Service.AI;

import com.chongcc.test.Result.ApiResponse;
import com.chongcc.test.dto.AiRequest;
import com.chongcc.test.dto.HstResponse;
import com.coze.openapi.client.chat.model.ChatEvent;
import io.reactivex.Flowable;

import java.util.List;


public interface AIPowerPointService {
    Flowable<ChatEvent> generatePowerPoint(AiRequest aiRequest) throws Exception;
    // 获取用户Hst列表
    ApiResponse<List<HstResponse>> getHstByUserId(Integer userId);

    ApiResponse<?> getHstById(String conversationId, String chatId);
    // 删除Hst
    void deleteHst(Integer id);
    // 获取PPT
    HstResponse findById(Integer id);
}
