package com.chongcc.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Getter    // 保证不可变性
public class AiServiceRequest {
    // 必须属性
    private final String model;
    private final double temperature;
    private final int maxTokens;
    // 非必须属性
    private boolean stream;
    private Message[] messages;
    @Data
    @AllArgsConstructor
    private static class Message {
        private String role;
        private String content;
    }

    private AiServiceRequest(RequestBuilder builder){
        this.model = builder.model;
        this.stream = builder.stream;
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.messages = builder.messages;
    }
    // 内部类建造者
    public static class RequestBuilder {
        // 必须属性
        private final String model;
        private final double temperature;
        private final int maxTokens;
        // 非必须属性
        private boolean stream = false;    // 添加默认stream值 false
        private Message[] messages = new Message[]{new Message("user", "请介绍自己")};
        public RequestBuilder(String model, double temperature, int maxTokens) {
            this.model = model;
            this.temperature = temperature;
            this.maxTokens = maxTokens;
        }
        public RequestBuilder stream(boolean stream) {
            this.stream = stream;
            return this;
        }
        public RequestBuilder message(String role, String content){
            this.messages = new Message[]{new Message(role, content)};
            return this;
        }
        public AiServiceRequest build(){
            return new AiServiceRequest(this);
        }
    }
}
