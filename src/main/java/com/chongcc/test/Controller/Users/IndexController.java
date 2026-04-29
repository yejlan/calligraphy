package com.chongcc.test.Controller.Users;

import com.chongcc.test.Result.ApiResponse;
import com.chongcc.test.Service.Users.UsersService;
import com.chongcc.test.dao.UserRepo;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class IndexController {
    private final UsersService usersService;
    public IndexController(UsersService usersService){
        this.usersService = usersService;
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestParam String username, @RequestParam String password){
        try {
            Map<String, Object> result =usersService.loginUser(username, password);
            return new ApiResponse<>(HttpStatus.OK.value(), "登录成功", result);
        } catch (RuntimeException e){
            if (e.getMessage().equals("用户不存在")){
                return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "用户不存在", null);
            } else if (e.getMessage().equals("密码错误")){
                return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "密码错误", null);
            }
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器错误", null);
        } catch (Exception e) {
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器错误", null);
        }
    }

    @PostMapping("/register")
    public ApiResponse<?> register(@RequestParam String username, @RequestParam String password, @RequestParam String role){
        try {
            usersService.registerUser(username, password, role);
            return new ApiResponse<>(HttpStatus.OK.value(), "注册成功", null);
        } catch (RuntimeException e){
            if (e.getMessage().equals("用户已存在")){
                return new ApiResponse<>(HttpStatus.CONFLICT.value(), "用户已存在", null);
            }
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器错误", null);
        } catch (Exception e) {
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器错误", null);
        }
    }
}
