package com.chongcc.test.Service.AI;

import java.util.concurrent.CountDownLatch;

public interface AIVideoService {
    /**
     * 建立WebSocket连接
     */
    void establishConnection();
    
    /**
     * 启动视频服务
     * @param countDownLatch 用于同步的CountDownLatch
     * @return 推流地址，如果获取失败返回null
     */
    String startVideoService(CountDownLatch countDownLatch) throws Exception;
    
    /**
     * 启动视频服务（无参数版本）
     * @return 推流地址，如果获取失败返回null
     */
    String startVideoService() throws Exception;
    
    /**
     * 发送文本交互请求
     */
    void sendTextInteraction(String text);
    
    /**
     * 关闭连接
     */
    void closeConnection();
    
    /**
     * 获取连接状态
     */
    boolean isConnected();
    
    /**
     * 获取当前推流地址
     * @return 推流地址，如果未获取到返回null
     */
    String getStreamUrl();
    
    /**
     * 检查是否已获取到推流地址
     * @return true表示已获取到推流地址
     */
    boolean isStreamUrlReceived();
}
