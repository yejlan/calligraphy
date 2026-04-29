package com.chongcc.test.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseMessage {
    private Integer courseId;
    private String courseName;
    private String courseCategory;
    private String courseDescription;
    private List<UserDTO> teachers; // 教师列表
    
    // 课程详情相关字段
    private List<DialHstResource> dialHstResources; // 绑定的对话历史资源
    private Integer participantCount; // 参与人数
    private String createdAt; // 创建时间
}
