package com.chongcc.test.Controller.AI;

import com.chongcc.test.Entity.ImgHst;
import com.chongcc.test.Result.ApiResponse;
import com.chongcc.test.Service.AI.AICalligraphyService;
import com.chongcc.test.dto.AiRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/calligraphy")
public class Calligraphy {
    private final AICalligraphyService AICalligraphyService;
    public Calligraphy(AICalligraphyService AICalligraphyService) {
        this.AICalligraphyService = AICalligraphyService;
    }

    @PostMapping("/image")
    public ResponseEntity<?> generateCalliImage(@RequestBody AiRequest aiRequest){
        return AICalligraphyService.generateCalligraphicImage(aiRequest);
    }
    @GetMapping("/hstList")
    public ResponseEntity<?> getHstByUserId(@RequestParam Integer userId){
        return AICalligraphyService.getHstByUserId(userId);
    }
    @GetMapping("/hst")
    public ResponseEntity<ApiResponse<String>> getGeneratedImgHst(@RequestParam Integer Id){
        return AICalligraphyService.getGeneratedImgHst(Id);
    }
}
