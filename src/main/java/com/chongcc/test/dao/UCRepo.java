package com.chongcc.test.dao;

import com.chongcc.test.Entity.Course;
import com.chongcc.test.Entity.User;
import com.chongcc.test.Entity.UserCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UCRepo extends JpaRepository<UserCourse, Integer> {
    @Query("SELECT uc.user FROM UserCourse uc WHERE uc.course = :course AND uc.user.role = 'teacher' ")
    List<User> findTeacherByCourse(@Param("course")Course course);

    @Modifying
    @Query("DELETE FROM UserCourse uc WHERE uc.courseId = :courseId")
    void deleteByCourseId(@Param("courseId") Integer courseId);

    @Query("SELECT uc.course FROM UserCourse uc WHERE uc.user = :user")
    List<Course> findCourseByUser(User user);
    
    // 获取课程的参与人数
    @Query("SELECT COUNT(uc) FROM UserCourse uc WHERE uc.courseId = :courseId")
    Long countParticipantsByCourseId(@Param("courseId") Integer courseId);
}
