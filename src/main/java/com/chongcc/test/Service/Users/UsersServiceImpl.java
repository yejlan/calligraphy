package com.chongcc.test.Service.Users;

import com.chongcc.test.Entity.User;
import com.chongcc.test.Util.JwtUtil;
import com.chongcc.test.dao.UserRepo;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class UsersServiceImpl implements UsersService{
    private final UserRepo userRepo;
    public UsersServiceImpl(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public void registerUser(String username, String password, String role) {
        try {
            if (userRepo.findByUsername(username).isPresent()) {
                throw new RuntimeException("用户已存在");
            }
            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            user.setRole(role);
            userRepo.save(user);
        } catch (RuntimeException e) {
            // 重新抛出业务异常
            throw e;
        } catch (Exception e) {
            // 处理其他技术异常
            throw new RuntimeException("注册失败");
        }
    }

    @Override
    public Map<String, Object> loginUser(String username, String password) {
        Optional<User> userOpt = userRepo.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }
        User user = userOpt.get();
        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("密码错误");
        }
        String token = JwtUtil.generateToken(username, user.getRole());
        return Map.of(
                "token", token,
                "userName", username,
                "permission", user.getRole()
        );
    }
}
