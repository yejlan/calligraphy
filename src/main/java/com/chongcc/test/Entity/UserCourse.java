package com.chongcc.test.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "user_course")
@IdClass(UserCourseId.class)
public class UserCourse {
    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Id
    @Column(name = "course_id")
    private Integer courseId;

    @ManyToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "course_id", insertable = false, updatable = false)
    private Course course;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
