package com.chongcc.test.Result;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;
    private String msg;
    private T data;
}
