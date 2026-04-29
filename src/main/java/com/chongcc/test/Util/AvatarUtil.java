package com.chongcc.test.Util;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Slf4j
public class AvatarUtil {
    
    /**
     * 创建WebSocket请求
     * @param requestUrl WebSocket连接URL
     * @return Request对象
     */
    public static Request createWebSocketRequest(String requestUrl) {
        return (new Request.Builder()).url(requestUrl).build();
    }
    
    /**
     * 创建OkHttpClient实例
     * @return OkHttpClient对象
     */
    public static OkHttpClient createOkHttpClient() {
        return new OkHttpClient().newBuilder().build();
    }
    
    /**
     * 创建WebSocket连接
     * @param requestUrl WebSocket连接URL
     * @param listener WebSocket监听器
     * @return WebSocket对象
     */
    public static WebSocket createWebSocket(String requestUrl, WebSocketListener listener) {
        Request wsRequest = createWebSocketRequest(requestUrl);
        OkHttpClient okHttpClient = createOkHttpClient();
        return okHttpClient.newWebSocket(wsRequest, listener);
    }
    
    /**
     * 发送WebSocket消息
     * @param webSocket WebSocket连接
     * @param request 要发送的JSON请求
     * @param isConnected 连接状态
     */
    public static void sendWebSocketMessage(WebSocket webSocket, JSONObject request, boolean isConnected) {
        if (isConnected && webSocket != null) {
            String jsonStr = JSONUtil.toJsonStr(request);
            log.info("send :{}", jsonStr);
            webSocket.send(jsonStr);
        }
    }
    
    /**
     * 关闭WebSocket连接
     * @param webSocket WebSocket连接
     */
    public static void closeWebSocket(WebSocket webSocket) {
        if (webSocket != null) {
            webSocket.close(1000, "");
        }
    }
    
    /**
     * 处理WebSocket消息
     * @param text 接收到的消息文本
     * @param countDownLatch 用于同步的CountDownLatch
     * @return 处理结果
     */
    public static boolean processWebSocketMessage(String text, CountDownLatch countDownLatch) {
        log.info("onMessage: {}", text);
        JSONObject jsonObject = JSON.parseObject(text);
        int code = jsonObject.getJSONObject("header").getIntValue("code");
        if (code != 0) {
            log.error("Server error: {}", jsonObject.getJSONObject("header").getString("message"));
            return false;
        }
        
        JSONObject payload = jsonObject.getJSONObject("payload");
        if (payload != null) {
            JSONObject avatar = payload.getJSONObject("avatar");
            if (avatar != null) {
                String streamUrl = avatar.getString("stream_url");
                if (streamUrl != null && !streamUrl.equals("")) {
                    System.out.println("获取到了推流地址，start成功");
                    if (countDownLatch != null) {
                        countDownLatch.countDown();
                    }
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 处理WebSocket事件
     * @param webSocket WebSocket连接
     * @param code 事件代码
     * @param reason 事件原因
     * @param event 事件类型
     * @param countDownLatch 用于同步的CountDownLatch
     */
    public static void handleWebSocketEvent(@NotNull WebSocket webSocket, int code, String reason, String event, CountDownLatch countDownLatch) {
        log.info("session {} . code:{}, reason:{}", event, code, reason);
        if (countDownLatch != null) {
            countDownLatch.countDown();
        }
        try {
            webSocket.close(code, reason);
        } catch (Exception var6) {
            log.error("{} error.{}", event, var6.getMessage());
        }
    }
    
    /**
     * 处理WebSocket失败事件
     * @param webSocket WebSocket连接
     * @param tx 异常对象
     * @param response HTTP响应
     */
    public static void handleWebSocketFailure(@NotNull WebSocket webSocket, @NotNull Throwable tx, Response response) {
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

    public static String assembleRequestUrl(String requestUrl, String apiKey, String apiSecret) {
        return assembleRequestUrl(requestUrl, apiKey, apiSecret, "GET");
    }

    public static String assembleRequestUrl(String requestUrl, String apiKey, String apiSecret, String method) {
        URL url = null;
        //转换WebSocket的URL，ws转为http，wss转为https
        String httpRequestUrl = requestUrl.replace("ws://", "http://").replace("wss://", "https://");
        try {
            url = new URL(httpRequestUrl);
            //设置时间格式并设置UTC时区
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            String date = format.format(new Date());
            String host = url.getHost();
            System.out.println("host:"+host);
            //构建签名字符串
            StringBuilder builder = new StringBuilder("host: ").append(host).append("\n").//
                    append("date: ").append(date).append("\n").//
                    append(method).append(" ").append(url.getPath()).append(" HTTP/1.1");

            System.out.println(builder.toString());
            System.out.println("--------------");
            Charset charset = Charset.forName("UTF-8");
            //生成 HMAC SHA-256 签名：
            Mac mac = Mac.getInstance("hmacsha256");
            SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
            mac.init(spec);
            byte[] hexDigits = mac.doFinal(builder.toString().getBytes(charset));
            String sha = Base64.getEncoder().encodeToString(hexDigits);
            //生产授权头信息，将授权信息编码为 Base64，并构建最终的请求 URL。
            String authorization = String.format("hmac username=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
            String authBase = Base64.getEncoder().encodeToString(authorization.getBytes(charset));
            System.out.println("signature:"+sha);
            System.out.println("----------------------------");
            System.out.println("authorization:"+authorization);
            System.out.println("--------------------");
            System.out.println("authBase:"+authBase);
            return String.format("%s?authorization=%s&host=%s&date=%s", requestUrl, URLEncoder.encode(authBase), URLEncoder.encode(host), URLEncoder.encode(date));
        } catch (Exception e) {
            throw new RuntimeException("assemble requestUrl error:" + e.getMessage());
        }
    }

    /**
     * 计算签名所需要的header参数 （http 接口）
     * @param requestUrl like 'http://rest-api.xfyun.cn/v2/iat'
     * @param apiKey
     * @param apiSecret
     * @method request method  POST/GET/PATCH/DELETE etc....
     * @param body   http request body
     * @return header map ，contains all headers should be set when access api
     */
    public static Map<String ,String> assembleRequestHeader(String requestUrl, String apiKey, String apiSecret, String method, byte[] body){
        URL url = null;
        try {
            url = new URL(requestUrl);
            // 获取日期
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            String date = format.format(new Date());
            //计算body 摘要(SHA256)
            MessageDigest instance = MessageDigest.getInstance("SHA-256");
            instance.update(body);
            String  digest = "SHA256="+ Base64.getEncoder().encodeToString(instance.digest());
            //date = "Thu, 19 Dec 2024 07:47:57 GMT";
            String host = url.getHost();
            int port = url.getPort(); // port >0 说明url 中带有port
            if (port > 0){
                host = host +":"+port;
            }
            String  path = url.getPath();
            if ("".equals(path) || path == null){
                path = "/";
            }
            //构建签名计算所需参数
            StringBuilder builder = new StringBuilder().
                    append("host: ").append(host).append("\n").//
                            append("date: ").append(date).append("\n").//
                            append(method).append(" ").append(path).append(" HTTP/1.1").append("\n").
                    append("digest: ").append(digest);
            System.out.println("builder:"+builder);
            Charset charset = Charset.forName("UTF-8");

            //使用hmac-sha256计算签名
            Mac mac = Mac.getInstance("hmacsha256");
            //System.out.println(builder.toString());
            SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
            mac.init(spec);
            byte[] hexDigits = mac.doFinal(builder.toString().getBytes(charset));
            String sha = Base64.getEncoder().encodeToString(hexDigits);
            // 构建header
            String authorization = String.format("hmac-auth api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line digest", sha);
            Map<String,String > header = new HashMap<String ,String>();
            System.out.println();

            header.put("authorization",authorization);
            header.put("host",host);
            header.put("date",date);
            header.put("digest",digest);
            System.out.println("header " + header.toString());
            return header;
        } catch (Exception e) {
            throw new RuntimeException("assemble requestHeader  error:"+e.getMessage());
        }
    }
}
