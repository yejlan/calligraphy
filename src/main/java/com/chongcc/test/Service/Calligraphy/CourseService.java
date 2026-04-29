package com.chongcc.test.Service.Calligraphy;

import com.chongcc.test.Entity.Course;
import com.chongcc.test.dto.CourseMessage;
import com.chongcc.test.dto.DialHstResource;

import org.springframework.data.domain.Page;

import java.util.List;

public interface CourseService {
    // 注册课程
    void register(CourseMessage courseMessage) throws Exception;
    // 获取所有课程(分页查询)
    Page<CourseMessage> getAllCourses(int pageNum, int pageSize) throws Exception;
    // 获取课程详情
    CourseMessage getCourseDetails(int courseId);
    // 用户参与课程
    void participate(int userId, int courseId);
    // 获取用户参与的课程
    List<Course> getUserParticipatedCourses(int userId);
    // 为课程添加对话记录（教案、PPT）
    void addDialHst(int courseId, int dialId) throws Exception;
    // 删除课程信息
    void deleteCourse(int courseId);
    // 获取课程绑定PPT
    List<Integer> getCoursePPTIds(int courseId);
}
