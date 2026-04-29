package com.chongcc.test.Service.AI;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.chongcc.test.Util.AvatarUtil;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class AIVideoServiceImpl implements AIVideoService{
    private final String appId;
    private final String apiKey;
    private final String url;
    private final String apiSecret;
    private final String avatarId;
    private final String VCN;
    private final String sceneId;
    
    // WebSocket 连接状态管理
    private WebSocket webSocket;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private CountDownLatch countDownLatch;
    private CountDownLatch connect;
    public static String vmr_status = "0";
    
    // 推流地址存储
    private String streamUrl;
    private final AtomicBoolean streamUrlReceived = new AtomicBoolean(false);
    
    // 心跳保活机制
    private Timer heartbeatTimer;
    private final AtomicBoolean heartbeatStarted = new AtomicBoolean(false);
    
    public AIVideoServiceImpl(
            @Value("${avatar.url}") String url,
            @Value("${avatar.apiKey}") String apiKey,
            @Value("${avatar.apiSecret}") String apiSecret,
            @Value("${avatar.appId}") String appId,
            @Value("${avatar.avatarId}") String avatar,
            @Value("${avatar.vcn}") String VCN,
            @Value("${avatar.sceneId}") String sceneId
    ){
        this.appId = appId;
        this.apiKey = apiKey;
        this.url = url;
        this.apiSecret = apiSecret;
        this.avatarId = avatar;
        this.VCN = VCN;
        this.sceneId = sceneId;
    }

    /**
     * 建立WebSocket连接
     */
    public void establishConnection() {
        // 检查是否已经连接
        if (isConnected.get()) {
            log.info("WebSocket连接已存在，跳过重复连接");
            return;
        }
        
        // 重置状态
        resetState();
        
        String request = AvatarUtil.assembleRequestUrl(url, apiKey, apiSecret);
        Request wsRequest = (new Request.Builder()).url(request).build();
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder().build();
        connect = new CountDownLatch(1);
        this.webSocket = okHttpClient.newWebSocket(wsRequest, buildWebSocketListener());
    }
    
    /**
     * 重置所有状态
     */
    private void resetState() {
        streamUrl = null;
        streamUrlReceived.set(false);
        isConnected.set(false);
        heartbeatStarted.set(false);
        
        // 清理之前的连接
        if (webSocket != null) {
            try {
                webSocket.close(1000, "重置连接");
            } catch (Exception e) {
                log.warn("关闭之前的WebSocket连接时出错: {}", e.getMessage());
            }
            webSocket = null;
        }
        
        // 停止心跳
        stopHeartbeat();
    }

    /**
     * 启动视频服务
     * @param countDownLatch 用于同步的CountDownLatch
     * @return 推流地址，如果获取失败返回null
     */
    public String startVideoService(CountDownLatch countDownLatch) throws Exception {
        //发送start帧
        System.out.println("开始发送start协议");
        this.countDownLatch = countDownLatch;
        System.out.println("connect之前");
        connect.await();
        System.out.println("connect之后");
        JSONObject startRequest = buildStartRequest();
        send(startRequest);
        
        // 启动心跳保活机制
        startHeartbeat();
        
        // 等待推流地址
        // 设置超时时间，避免无限等待
        long startTime = System.currentTimeMillis();
        long timeout = 30000; // 30秒超时
        
        while (!streamUrlReceived.get() && (System.currentTimeMillis() - startTime) < timeout) {
            Thread.sleep(100); // 每100ms检查一次
        }
        
        if (streamUrlReceived.get()) {
            return streamUrl;
        } else {
            log.error("获取推流地址超时");
            return null;
        }
    }

    /**
     * 启动视频服务（无参数版本）
     * @return 推流地址，如果获取失败返回null
     */
    public String startVideoService() throws Exception {
        System.out.println("connect之前");
        connect.await();
        System.out.println("connect之后");
        JSONObject startRequest = buildStartRequest();
        send(startRequest);
        
        // 启动心跳保活机制
        startHeartbeat();
        
        // 等待推流地址
        long startTime = System.currentTimeMillis();
        long timeout = 30000; // 30秒超时
        
        while (!streamUrlReceived.get() && (System.currentTimeMillis() - startTime) < timeout) {
            Thread.sleep(100);
        }
        
        if (streamUrlReceived.get()) {
            return streamUrl;
        } else {
            log.error("获取推流地址超时");
            return null;
        }
    }

    /**
     * 启动心跳保活机制
     */
    private void startHeartbeat() {
        if (heartbeatStarted.compareAndSet(false, true)) {
            //发送ping帧,start之后每5秒发送一次ping心跳，用来维持ws连接
            heartbeatTimer = new Timer("HeartbeatTimer", true); // 使用守护线程
            CompletableFuture.runAsync(() -> {
                TimerTask timeoutTask = new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            if (isConnected.get() && webSocket != null) {
                                send(buildPingRequest());
                                log.debug("发送心跳ping帧");
                            } else {
                                // 如果连接断开，停止心跳
                                log.info("连接已断开，停止心跳");
                                stopHeartbeat();
                            }
                        } catch (Exception e) {
                            log.error("心跳发送失败: {}", e.getMessage());
                            // 心跳失败，停止心跳
                            stopHeartbeat();
                        }
                    }
                };
                heartbeatTimer.scheduleAtFixedRate(timeoutTask, 0, 5000);
            });
            log.info("心跳保活机制已启动");
        }
    }
    
    /**
     * 停止心跳保活机制
     */
    private void stopHeartbeat() {
        if (heartbeatStarted.compareAndSet(true, false)) {
            if (heartbeatTimer != null) {
                try {
                    heartbeatTimer.cancel();
                    heartbeatTimer.purge(); // 清理已取消的任务
                } catch (Exception e) {
                    log.warn("停止心跳定时器时出错: {}", e.getMessage());
                } finally {
                    heartbeatTimer = null;
                }
            }
            log.info("心跳保活机制已停止");
        }
    }

    /**
     * 发送消息
     */
    public void send(JSONObject request) {
        if (!isConnected.get()) {
            log.warn("WebSocket未连接，无法发送消息");
            return;
        }
        
        if (webSocket == null) {
            log.warn("WebSocket对象为空，无法发送消息");
            return;
        }
        
        try {
            String jsonStr = JSONUtil.toJsonStr(request);
//            log.info("send :{}", jsonStr);
            webSocket.send(jsonStr);
        } catch (Exception e) {
            log.error("发送消息失败: {}", e.getMessage());
            // 如果发送失败，可能需要重新连接
            isConnected.set(false);
        }
    }

    /**
     * 发送文本交互请求
     */
    public void sendTextInteraction(String text) {
        JSONObject textRequest = buildTextinteractRequest(text);
        send(textRequest);
    }

    /**
     * 关闭连接
     */
    public void closeConnection() {
        log.info("开始关闭WebSocket连接");
        
        // 停止心跳
        stopHeartbeat();
        
        // 重置状态
        isConnected.set(false);
        streamUrlReceived.set(false);
        
        // 关闭WebSocket连接
        if (this.webSocket != null) {
            try {
                this.webSocket.close(1000, "正常关闭");
                this.webSocket = null;
            } catch (Exception e) {
                log.warn("关闭WebSocket连接时出错: {}", e.getMessage());
            }
        }
        
        // 清理同步锁
        if (connect != null) {
            connect.countDown();
        }
        if (countDownLatch != null) {
            countDownLatch.countDown();
        }
        
        log.info("WebSocket连接已关闭");
    }

    /**
     * 获取连接状态
     */
    public boolean isConnected() {
        return isConnected.get();
    }
    
    /**
     * 获取当前推流地址
     * @return 推流地址，如果未获取到返回null
     */
    public String getStreamUrl() {
        return streamUrl;
    }
    
    /**
     * 检查是否已获取到推流地址
     * @return true表示已获取到推流地址
     */
    public boolean isStreamUrlReceived() {
        return streamUrlReceived.get();
    }

    /**
     * 构建WebSocket监听器
     */
    private WebSocketListener buildWebSocketListener() {
        return new WebSocketListener() {
            //处理连接打开事件
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                log.info("onOpen");
                System.out.println("触发onOpen事件，连接上了");
                isConnected.set(true);
                connect.countDown();
            }
            
            //处理接受消息事件
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
//                log.info("onMessage: {}", text);
                JSONObject jsonObject = JSON.parseObject(text);
                int code = jsonObject.getJSONObject("header").getIntValue("code");
                if (code != 0) {
                    onEvent(webSocket, 1002, jsonObject.getJSONObject("header").getString("message"), "server closed");
                    System.exit(0);
                    return;
                }
                JSONObject payload = jsonObject.getJSONObject("payload");
                if (payload != null) {
                    JSONObject avatar = payload.getJSONObject("avatar");
                    if (avatar != null) {
                        String receivedStreamUrl = avatar.getString("stream_url");
                        if (receivedStreamUrl != null && !receivedStreamUrl.equals("")) {
                            System.out.println("获取到了推流地址，start成功");
                            // 保存推流地址
                            streamUrl = receivedStreamUrl;
                            streamUrlReceived.set(true);
                            if (countDownLatch != null) {
                                countDownLatch.countDown();
                            }
                        }
                    }
                }
            }
            
            //处理连接关闭事件
            public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                this.onEvent(webSocket, code, reason, "onClosing");
            }
            
            //处理连接已关闭事件
            public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                this.onEvent(webSocket, code, reason, "onClosed");
            }
            
            //处理连接失败事件
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable tx, Response response) {
                Object t;
                try {
                    String responseBody = response.body().string();
                    JSONObject body = JSON.parseObject(responseBody);
                    t = new ProtocolException(body.toString());
                } catch (IOException var6) {
                    t = var6;
                }
                log.info("onFailure:{}", t);
            }
            
            //处理其他事件
            void onEvent(@NotNull WebSocket webSocket, int code, String reason, String event) {
                log.info("session {} . code:{}, reason:{}", event, code, reason);
                isConnected.set(false);
                // 停止心跳
                stopHeartbeat();
                if (countDownLatch != null) {
                    countDownLatch.countDown();
                }
                try {
                    webSocket.close(code, reason);
                } catch (Exception var6) {
                    log.error("{} error.{}", event, var6.getMessage());
                }
            }
        };
    }

    //启动协议
    private JSONObject buildStartRequest() {
        JSONObject header = new JSONObject()
                .fluentPut("app_id", appId)
                .fluentPut("ctrl", "start")//控制参数
                .fluentPut("request_id", UUID.randomUUID().toString())
                .fluentPut("scene_id",sceneId);//请到交互平台-接口服务中获取，传入"接口服务ID"

        JSONObject parameter = new JSONObject()
                .fluentPut("avatar", new JSONObject()
                        .fluentPut("avatar_id", avatarId)// （必传）授权的形象资源id，请到交互平台-接口服务-形象列表中获取
                        .fluentPut("width",720)// 视频分辨率：宽
                        .fluentPut("height",1280)// 视频分辨率：高
                        .fluentPut("stream", new JSONObject()
                                .fluentPut("protocol", "flv")//（必传）视频协议，支持rtmp，xrtc、webrtc、flv，目前只有xrtc支持透明背景，需配合alpha参数传1
                                .fluentPut("fps",25)// （非必传）视频刷新率,值越大，越流畅，取值范围0-25，默认25即可
                                .fluentPut("bitrate",5000)))//（非必传）视频码率，值越大，越清晰，对网络要求越高
//                                .fluentPut("alpha",0)))//（非必传）透明背景，需配合protocol=xrtc，0关闭，1开启
                .fluentPut("tts",new JSONObject()
                        .fluentPut("speed",50)// 语速：[0,100]，默认50
                        .fluentPut("vcn",VCN))//（必传）授权的声音资源id，请到交互平台-接口服务-声音列表中获取
                .fluentPut("subtitle",new JSONObject()//注意：由于是云端发送的字幕，因此无法获取虚拟人具体读到哪个字了，也无法暂停和续播
                        .fluentPut("subtitle",0)//0关闭，1开启
                        .fluentPut("font_color","#FF0000")//字体颜色
                        .fluentPut("font_size",10)//字体大小，取值范围：1-10
                        .fluentPut("position_x",0)//字幕左右移动，必须配合width、height一起传
                        .fluentPut("position_y",0)//字幕上下移动，必须配合width、height一起传
                        .fluentPut("font_name","mainTitle")//字体样式，目前有以下字体：
//'Sanji.Suxian.Simple','Honglei.Runninghand.Sim','Hunyuan.Gothic.Bold',
//'Huayuan.Gothic.Regular','mainTitle'
                        .fluentPut("width",100)//字幕宽
                        .fluentPut("height",100));//字幕高
        return new JSONObject().fluentPut("header",header).fluentPut("parameter",parameter);
    }
    
    //文本交互协议
    private JSONObject buildTextinteractRequest(String text){
        JSONObject header = new JSONObject()
                .fluentPut("app_id",appId)
                .fluentPut("ctrl","text_interact")
                .fluentPut("request_id", UUID.randomUUID().toString());

        JSONObject parameter = new JSONObject()
                .fluentPut("tts",new JSONObject()
                        .fluentPut("vcn",VCN)
                        .fluentPut("speed",50)
                        .fluentPut("pitch",50)
                        .fluentPut("audio",new JSONObject()
                                .fluentPut("sample_rate",16000)))
                .fluentPut("air",new JSONObject()
                        .fluentPut("air",1)//是否开启自动动作，0关闭/1开启，自动动作只有开启交互走到大模型时才生效
                        //星火大模型会根据语境自动插入动作，且必须是支持动作的形象
                        .fluentPut("add_nonsemantic",1));//是否开启无指向性动作，0关闭，1开启（需配合nlp=true时生效)，虚拟人会做没有意图指向性的动作

        JSONObject payload = new JSONObject()
                .fluentPut("text",new JSONObject()
                        .fluentPut("content",text));
        return new JSONObject().fluentPut("header",header).fluentPut("parameter",parameter).fluentPut("payload",payload);
    }
    //心跳，保活协议
    private JSONObject buildPingRequest() {
        JSONObject header = new JSONObject()
                .fluentPut("app_id",appId)
                .fluentPut("ctrl","ping")
                .fluentPut("request_id", UUID.randomUUID().toString());
        return new JSONObject().fluentPut("header",header);
    }

    @PreDestroy
    public void destroy() {
        log.info("AIVideoServiceImpl is being destroyed.");
        closeConnection();
        stopHeartbeat();
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
            heartbeatTimer.purge();
        }
        log.info("AIVideoServiceImpl resources cleaned up.");
    }
}
