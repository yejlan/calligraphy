package com.chongcc.test.Service.Calligraphy;

import com.chongcc.test.Entity.Course;
import com.chongcc.test.Entity.DialHst;
import com.chongcc.test.Entity.User;
import com.chongcc.test.Entity.UserCourse;
import com.chongcc.test.dao.*;
import com.chongcc.test.dto.CourseMessage;
import jakarta.persistence.EntityExistsException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.chongcc.test.dto.DialHstResource;

@Service
public class CourseServiceImpl implements CourseService{
    private static final Logger log = LoggerFactory.getLogger(CourseServiceImpl.class);
    private final CourseRepo courseRepo;
    private final UserRepo userRepo;
    private final UCRepo ucRepo;
    private final DialHstRepo dialHstRepo;
    private final CourseDialHstMapper cDHMapper;
    public CourseServiceImpl(CourseRepo courseRepo, UserRepo userRepo, UCRepo ucRepo, CourseDialHstMapper cDHMapper, DialHstRepo dialHstRepo) {
        this.courseRepo = courseRepo;
        this.userRepo = userRepo;
        this.ucRepo = ucRepo;
        this.dialHstRepo = dialHstRepo;
        this.cDHMapper = cDHMapper;
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public void register(CourseMessage courseMessage) throws Exception {
        System.out.println("register course:" + courseMessage);
        try {
            Course course = new Course();
            course.setId(courseMessage.getCourseId());
            course.setName(courseMessage.getCourseName());
            course.setCategory(courseMessage.getCourseCategory());
            course.setDescription(courseMessage.getCourseDescription());
            courseRepo.save(course);    // 创建课程
            System.out.println("Saved course ID: " + course.getId()); // 调试信息
            if(courseMessage.getTeachers() != null && !courseMessage.getTeachers().isEmpty()){    // 教学关系绑定
                courseMessage.getTeachers().stream()
                        .filter(teacherDTO -> userRepo.existsById(teacherDTO.getId()))
                        .forEach(teacherDTO -> {
                            Optional<User> teacherOpt = userRepo.findById(teacherDTO.getId());
                            teacherOpt.ifPresent(teacher -> {
                                UserCourse teachCourse = new UserCourse();
                                teachCourse.setUserId(teacher.getId());     // 为什么不能直接赋值对象？
                                teachCourse.setCourseId(course.getId());
                                teachCourse.setCreatedAt(LocalDateTime.now());
                                System.out.println("Saving UserCourse - Course ID: " + course.getId() + ", User ID: " + teacher.getId());
                                ucRepo.save(teachCourse);
                            });
                        });
            } else {
                throw new RuntimeException("Teacher does not exist");
            }
        } catch (EntityExistsException e){
            System.out.println("Course already exists");
            throw new EntityExistsException("Course already exists");
        } catch (DataIntegrityViolationException e) {
            System.out.println("Invalid integrity constraint!");
            throw new DataIntegrityViolationException("Invalid integrity constraint!");
        } catch (Exception e){
            System.out.println(e.getMessage());
            throw new Exception(e.getMessage());
        }
    }

    @Override
    public Page<CourseMessage> getAllCourses(int pageNum, int pageSize) throws Exception {
        try{
            if (pageNum < 0 || pageSize < 0){
                throw new IllegalArgumentException("Invalid page number or page size");
            }
            Pageable pageable = PageRequest.of(pageNum, pageSize);
            Page<Course> courses = courseRepo.findAll(pageable);
            return courses.map(course -> {
                        CourseMessage courseMessage = new CourseMessage();
                        courseMessage.setCourseId(course.getId());
                        courseMessage.setCourseName(course.getName());
                        courseMessage.setCourseCategory(course.getCategory());
                        courseMessage.setCourseDescription(course.getDescription());
                        try{
                            List<User> teachers = ucRepo.findTeacherByCourse(course);
                            if (teachers != null) {
                                courseMessage.setTeachers(
                                        teachers.stream()
                                                .map(User::toDTO)
                                                .filter(Objects::nonNull)
                                                .toList()
                                );
                            }
                        } catch (Exception e){
                            log.warn("Failed to fetch teachers for course");
                            courseMessage.setTeachers(List.of());
                        }
                        return courseMessage;
                    });
        } catch (IllegalArgumentException e){
            throw new Exception("Invalid pagination parameters: " + e.getMessage());
        } catch (DataAccessException e) {
            throw new Exception("Database access error: " + e.getMessage());
        } catch (Exception e) {
            throw new Exception("Failed to retrieve courses: " + e.getMessage());
        }
    }

    @Override
    public CourseMessage getCourseDetails(int courseId) {
        try {
            // 1. 获取课程基本信息
            Optional<Course> courseOpt = courseRepo.findById(courseId);
            if (!courseOpt.isPresent()) {
                throw new RuntimeException("Course not found");
            }
            
            Course course = courseOpt.get();
            CourseMessage courseMessage = new CourseMessage();
            courseMessage.setCourseId(course.getId());
            courseMessage.setCourseName(course.getName());
            courseMessage.setCourseCategory(course.getCategory());
            courseMessage.setCourseDescription(course.getDescription());
            
            // 2. 获取教师信息
            try {
                List<User> teachers = ucRepo.findTeacherByCourse(course);
                if (teachers != null) {
                    courseMessage.setTeachers(
                            teachers.stream()
                                    .map(User::toDTO)
                                    .filter(Objects::nonNull)
                                    .toList()
                    );
                }
            } catch (Exception e) {
                log.warn("Failed to fetch teachers for course: {}", e.getMessage());
                courseMessage.setTeachers(List.of());
            }
            
            // 3. 获取参与人数
            try {
                Long participantCount = ucRepo.countParticipantsByCourseId(courseId);
                courseMessage.setParticipantCount(participantCount != null ? participantCount.intValue() : 0);
            } catch (Exception e) {
                log.warn("Failed to fetch participant count for course: {}", e.getMessage());
                courseMessage.setParticipantCount(0);
            }
            
            // 4. 获取绑定的对话历史资源
            try {
                List<Integer> dialIds = cDHMapper.queryDialIdListByCourseId(courseId);
                List<DialHstResource> dialHstResources = dialIds.stream()
                        .map(dialId -> {
                            Optional<DialHst> dialHstOpt = dialHstRepo.findById(dialId);
                            if (dialHstOpt.isPresent()) {
                                DialHst dialHst = dialHstOpt.get();
                                DialHstResource resource = new DialHstResource();
                                resource.setId(dialHst.getId());
                                resource.setDialType(dialHst.getDialType());
                                resource.setQuestion(dialHst.getQuestion());
                                resource.setConversationId(dialHst.getConversationId());
                                resource.setChatId(dialHst.getChatId());
                                resource.setUserId(dialHst.getUserId());
                                return resource;
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .toList();
                courseMessage.setDialHstResources(dialHstResources);
            } catch (Exception e) {
                log.warn("Failed to fetch dial history resources for course: {}", e.getMessage());
                courseMessage.setDialHstResources(List.of());
            }
            
            return courseMessage;
            
        } catch (Exception e) {
            log.error("Failed to get course details for courseId {}: {}", courseId, e.getMessage());
            throw new RuntimeException("Failed to get course details: " + e.getMessage());
        }
    }

    @Override
    public void participate(int userId, int courseId) {

    }

    @Override
    public List<Course> getUserParticipatedCourses(int userId) {
        if(!userRepo.existsById(userId)){
            throw new RuntimeException("User is not exists");
        }
        return ucRepo.findCourseByUser(userRepo.findById(userId).get());
    }

    @Override
    public void addDialHst(int courseId, int dialId) throws Exception {
        try {
            Optional<DialHst> dialHstOpt = dialHstRepo.findById(dialId);
            Optional<Course> courseOpt = courseRepo.findById(courseId);
            if (!dialHstOpt.isPresent()) {
                throw new RuntimeException("DialHst is not exists");
            }
//            System.out.println("DialHst:" + dialHstOpt.get());
            if (!courseOpt.isPresent()) {
                throw new RuntimeException("Course is not exists");
            }
//            System.out.println("Course:" + courseOpt.get());
            cDHMapper.addCDH(courseId, dialId);
        } catch (DuplicateKeyException e) {
            throw new DuplicateKeyException("Duplicate data insertion");
        } catch (Exception e){
            throw new Exception(e.getMessage());
        }
    }
    @Override
    @Transactional(rollbackOn = Exception.class)
    public void deleteCourse(int courseId) {
        try {
            if(!courseRepo.existsById(courseId)){
                throw new RuntimeException("Course is not exists");
            }
            System.out.println("delete course:" + courseId);
            // 1) 删除 user_course 关联（不删除用户本身）
            ucRepo.deleteByCourseId(courseId);
            // 2) 删除 course_dialhst 关联（不删除对话或用户本身）
            cDHMapper.deleteByCourseId(courseId);
            // 3) 删除课程记录
            courseRepo.deleteById(courseId);
        } catch (Exception e) {
            log.error("Failed to delete course {}: {}", courseId, e.getMessage());
            throw e;
        }
    }
    @Override
    public List<Integer> getCoursePPTIds(int courseId) {
        if(!courseRepo.existsById(courseId)){
            throw new RuntimeException("Course is not exists");
        }
        List<Integer> dialIds = cDHMapper.queryDialIdListByCourseId(courseId);
        return dialIds.stream()
                .filter(dialId -> dialHstRepo.findById(dialId).get().getDialType().equals("ppt"))
                .toList();
    }
}
