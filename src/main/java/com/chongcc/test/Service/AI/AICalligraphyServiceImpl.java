package com.chongcc.test.Service.AI;

import com.chongcc.test.Entity.ImgHst;
import com.chongcc.test.dao.ImgHstRepo;
import com.chongcc.test.Result.ApiResponse;
import com.chongcc.test.dto.AiRequest;
import com.chongcc.test.dto.HstResponse;
import com.volcengine.ark.runtime.model.images.generation.GenerateImagesRequest;
import com.volcengine.ark.runtime.model.images.generation.ImagesResponse;
import com.volcengine.ark.runtime.service.ArkService;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AICalligraphyServiceImpl implements AICalligraphyService {
    private final ImgHstRepo imgHstRepo;
    private final String model;
    private final String apiKey;
    private final String prompt;
    public AICalligraphyServiceImpl(
            ImgHstRepo imgHstRepo,
            @Value("${volc.img-model}") String model,
            @Value("${volc.api-key}") String apiKey,
            @Value("${volc.prompt}") String prompt) {
        this.imgHstRepo = imgHstRepo;
        this.model = model;
        this.apiKey = apiKey;
        this.prompt = prompt;
            }
    @Override
    public ResponseEntity<?> generateCalligraphicImage(AiRequest aiRequest) {
        ConnectionPool connectionPool = null;
        ArkService service = null;
        try{
            connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
            Dispatcher dispatcher = new Dispatcher();
            service = ArkService.builder().dispatcher(dispatcher).connectionPool(connectionPool).apiKey(apiKey).build();

            GenerateImagesRequest generateRequest = GenerateImagesRequest.builder()
                    .model(model)
                    .prompt(aiRequest.getContent() + prompt)
                    .build();
            ImagesResponse imagesResponse = service.generateImages(generateRequest);
            String imageUrl = imagesResponse.getData().get(0).getUrl();
            if (aiRequest.isAutoSaveHistory())
            {
                ImgHst imgHst = new ImgHst();
                imgHst.setFilePath(imageUrl);
                imgHst.setUserId(Integer.valueOf(aiRequest.getUserId()));
                imgHst.setPrompt(aiRequest.getContent());
                imgHstRepo.save(imgHst);
            }
            return ResponseEntity.ok()
                    .body(new ApiResponse<>(
                            200,
                            "success",
                            imageUrl
                    ));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(
                            500,
                            "Failed to generate image: " + e.getMessage(),
                            null
                    ));
        } finally {
            // 5. 确保资源释放
            if (service != null) {
                service.shutdownExecutor();
            }
            if (connectionPool != null) {
                connectionPool.evictAll(); // 清理连接池
            }
        }
    }
    @Override
    public ResponseEntity<ApiResponse<List<HstResponse>>> getHstByUserId(Integer userId) {
        try {
            List<ImgHst> imgHsts = imgHstRepo.findByUserId(userId);
            List<HstResponse> hstResponseList = imgHsts.stream()
                    .map(imgHst -> new HstResponse(
                            imgHst.getId(),
                            "Image",
                            imgHst.getPrompt(),
                            null,
                            null,
                            imgHst.getFilePath(),
                            imgHst.getUserId()
                    ))
                    .toList();
            return ResponseEntity.ok(
                    new ApiResponse<>(
                            200,
                            "success",
                            hstResponseList
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(404).body(
                    new ApiResponse<>(
                            404,
                            "未找到字帖生成历史 " + e.getMessage(),
                            null
                    )
            );
        }
    }
    @Override
    public ResponseEntity<ApiResponse<String>> getGeneratedImgHst(Integer Id) {
        try {
            ImgHst imgHst = imgHstRepo.findImgHstById(Id);
            return ResponseEntity.ok(
                    new ApiResponse<>(
                            200,
                            "成功获取字帖生成历史",
                            imgHst.getFilePath()
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(
                            404,
                            "未找到对应图片 " + e.getMessage(),
                            null
                    ));
        }
    }

}
