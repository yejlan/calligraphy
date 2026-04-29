package com.chongcc.test.Entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserCourseId implements Serializable {
    private Integer userId;
    private Integer courseId;
}
