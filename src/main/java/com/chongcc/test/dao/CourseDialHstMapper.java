package com.chongcc.test.dao;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseDialHstMapper {
    // 查询课程对应文件列表
    @Select("SELECT dial_id FROM course_dialhst WHERE course_id = #{courseId}")
    List<Integer> queryDialIdListByCourseId(Integer courseId);
    // 添加课程PPT、视频
    @Insert("INSERT INTO course_dialhst VALUES (#{courseId}, #{dialId})")
    void addCDH(Integer courseId, Integer dialId);

    // 删除课程与对话记录的关联
    @Delete("DELETE FROM course_dialhst WHERE course_id = #{courseId}")
    void deleteByCourseId(Integer courseId);
}
