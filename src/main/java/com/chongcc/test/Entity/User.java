package com.chongcc.test.Entity;

import com.chongcc.test.dto.UserDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "user")
public class User {
    @Id
    private int id;
    private String username;
    private String password;
    private String role;
    @Column(name = "register_time")
    private LocalDateTime registerTime;

    public UserDTO toDTO(){
        UserDTO userDTO = new UserDTO();
        userDTO.setId(id);
        userDTO.setUsername(username);
        userDTO.setRole(role);
        return userDTO;
    }
}
