package com.chongcc.test.dto;

import lombok.Data;

@Data
public class AiRequest {
    private String userId;
    private boolean stream = true;
    private boolean autoSaveHistory = true;
    private String content;
}
