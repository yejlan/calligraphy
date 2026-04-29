package com.chongcc.test.dto;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class HstResponse {
    private Integer id;
    private String dialType;
    private String question;
    private String conversationId;
    private String chatId;
    private String answer;
    private Integer userId;
}
