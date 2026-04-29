package com.chongcc.test.Service.Users;

import com.chongcc.test.Result.ApiResponse;

import java.util.Map;

public interface UsersService {
    void registerUser(String username, String password, String role);

    Map<String, Object> loginUser(String username, String password);
}
