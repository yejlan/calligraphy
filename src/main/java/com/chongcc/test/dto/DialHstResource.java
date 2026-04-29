package com.chongcc.test.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DialHstResource {
    private Integer id;
    private String dialType; // ppt, chat, copybook, video等
    private String question; // 用户问题
    private String answer; // AI回答
    private String conversationId;
    private String chatId;
    private Integer userId;
    private String downloadUrl; // 下载链接（如果有)
}
