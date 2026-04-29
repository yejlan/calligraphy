package com.chongcc.test.dao;

import com.chongcc.test.Entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepo extends JpaRepository<Course, Integer> {
    Course findByName(String name);
}
