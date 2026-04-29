package com.chongcc.test.Controller.AI;

import com.chongcc.test.Result.ApiResponse;
import com.chongcc.test.Service.AI.AIVideoService;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CountDownLatch;

@RequestMapping("/video")
@RestController
public class VideoController {
    private final AIVideoService aiVideoService;
    public VideoController(AIVideoService aiVideoService)
    {
        this.aiVideoService = aiVideoService;
    }

    /**
     * 返回视频测试页面
     * @return 测试页面
     */
    @GetMapping("/test")
    public String getTestPage() {
        return "redirect:/video-test.html";
    }


    
    /**
     * 启动视频服务并获取推流地址
     * @return 包含推流地址的响应
     */
    @PostMapping("/start")
    public ApiResponse<String> startVideoService() {
        try {
            // 建立连接
            aiVideoService.establishConnection();
            
            // 启动视频服务并获取推流地址
            String streamUrl = aiVideoService.startVideoService();
            
            if (streamUrl != null) {
                return new ApiResponse<>(200, "视频服务启动成功", streamUrl);
            } else {
                return new ApiResponse<>(500, "获取推流地址失败", null);
            }
        } catch (Exception e) {
            return new ApiResponse<>(500, "启动视频服务失败: " + e.getMessage(), null);
        }
    }
    
    /**
     * 启动视频服务并获取推流地址（带同步机制）
     * @return 包含推流地址的响应
     */
    @PostMapping("/start-sync")
    public ApiResponse<String> startVideoServiceWithSync() {
        try {
            // 建立连接
            aiVideoService.establishConnection();
            
            // 创建同步锁
            CountDownLatch latch = new CountDownLatch(1);
            
            // 启动视频服务并获取推流地址
            String streamUrl = aiVideoService.startVideoService(latch);
            
            if (streamUrl != null) {
                return new ApiResponse<>(200, "视频服务启动成功", streamUrl);
            } else {
                return new ApiResponse<>(500, "获取推流地址失败", null);
            }
        } catch (Exception e) {
            return new ApiResponse<>(500, "启动视频服务失败: " + e.getMessage(), null);
        }
    }
    
    /**
     * 发送文本交互
     * @param text 要发送的文本
     * @return 操作结果
     */
    @PostMapping("/interact")
    public ApiResponse<String> sendTextInteraction(@RequestParam String text) {
        try {
            if (!aiVideoService.isConnected()) {
                return new ApiResponse<>(400, "WebSocket连接未建立", null);
            }
            
            aiVideoService.sendTextInteraction(text);
            return new ApiResponse<>(200, "文本交互发送成功", null);
        } catch (Exception e) {
            return new ApiResponse<>(500, "发送文本交互失败: " + e.getMessage(), null);
        }
    }
    
    /**
     * 获取当前推流地址
     * @return 推流地址
     */
    @GetMapping("/stream-url")
    public ApiResponse<String> getStreamUrl() {
        String streamUrl = aiVideoService.getStreamUrl();
        if (streamUrl != null) {
            return new ApiResponse<>(200, "获取推流地址成功", streamUrl);
        } else {
            return new ApiResponse<>(404, "推流地址未获取", null);
        }
    }
    
    /**
     * 关闭视频服务连接
     * @return 操作结果
     */
    @PostMapping("/close")
    public ApiResponse<String> closeVideoService() {
        try {
            aiVideoService.closeConnection();
            return new ApiResponse<>(200, "视频服务连接已关闭", null);
        } catch (Exception e) {
            return new ApiResponse<>(500, "关闭连接失败: " + e.getMessage(), null);
        }
    }
    
    /**
     * 集成接口：接收互动提示词，启动视频服务，发送互动协议，返回推流地址
     * 前端调用逻辑：输入互动提示词 -> 后端接收 -> 启动视频服务 -> 推送互动协议 -> 返回推流地址
     * @param request 包含互动提示词的请求
     * @return 包含推流地址的响应
     */
    @PostMapping("/interactive-start")
    public ApiResponse<String> interactiveVideoStart(@RequestParam String request) {
        try {
            // 检查输入参数
            if (request == null || request.trim().isEmpty()) {
                return new ApiResponse<>(400, "互动提示词不能为空", null);
            }
            
            // 1. 建立WebSocket连接
            aiVideoService.establishConnection();
            
            // 2. 启动视频服务并获取推流地址
            String streamUrl = aiVideoService.startVideoService();
            
            if (streamUrl == null) {
                return new ApiResponse<>(500, "获取推流地址失败", null);
            }
            
            // 3. 等待一小段时间确保服务完全启动
            Thread.sleep(1000);
            
            // 4. 检查连接状态
            if (!aiVideoService.isConnected()) {
                return new ApiResponse<>(500, "视频服务连接已断开", null);
            }
            
            // 5. 发送互动提示词
            aiVideoService.sendTextInteraction(request);
            
            // 6. 返回推流地址
            return new ApiResponse<>(200, "视频服务启动成功，互动协议已发送", streamUrl);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ApiResponse<>(500, "操作被中断: " + e.getMessage(), null);
        } catch (Exception e) {
            return new ApiResponse<>(500, "启动交互式视频服务失败: " + e.getMessage(), null);
        }
    }
    
    /**
     * 集成接口：带同步机制的版本
     * @param request 包含互动提示词的请求
     * @return 包含推流地址的响应
     */
    @PostMapping("/interactive-start-sync")
    public ApiResponse<String> interactiveVideoStartWithSync(@RequestParam String request) {
        try {
            // 检查输入参数
            if (request == null || request.trim().isEmpty()) {
                return new ApiResponse<>(400, "互动提示词不能为空", null);
            }
            
            // 1. 建立WebSocket连接
            aiVideoService.establishConnection();
            
            // 2. 创建同步锁
            CountDownLatch latch = new CountDownLatch(1);
            
            // 3. 启动视频服务并获取推流地址
            String streamUrl = aiVideoService.startVideoService(latch);
            
            if (streamUrl == null) {
                return new ApiResponse<>(500, "获取推流地址失败", null);
            }
            
            // 4. 等待一小段时间确保服务完全启动
            Thread.sleep(1000);
            
            // 5. 检查连接状态
            if (!aiVideoService.isConnected()) {
                return new ApiResponse<>(500, "视频服务连接已断开", null);
            }
            
            // 6. 发送互动提示词
            aiVideoService.sendTextInteraction(request);
            
            // 7. 返回推流地址
            return new ApiResponse<>(200, "视频服务启动成功，互动协议已发送", streamUrl);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ApiResponse<>(500, "操作被中断: " + e.getMessage(), null);
        } catch (Exception e) {
            return new ApiResponse<>(500, "启动交互式视频服务失败: " + e.getMessage(), null);
        }
    }
}
