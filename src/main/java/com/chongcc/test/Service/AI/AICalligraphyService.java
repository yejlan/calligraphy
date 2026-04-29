package com.chongcc.test.Service.AI;

import com.chongcc.test.Entity.ImgHst;
import com.chongcc.test.Result.ApiResponse;
import com.chongcc.test.dto.AiRequest;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface AICalligraphyService {
    /**
     * 功能：使用火山引擎进行文生图完成字帖生成
     * @param
     * @return 目前返回为图片url
     */
    ResponseEntity<?> generateCalligraphicImage(AiRequest aiRequest);

    ResponseEntity<?> getHstByUserId(Integer userId);
    ResponseEntity<ApiResponse<String>> getGeneratedImgHst(Integer Id);
}
