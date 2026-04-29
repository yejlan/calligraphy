package com.chongcc.test.Controller;

import com.chongcc.test.Entity.Course;
import com.chongcc.test.Entity.DialHst;
import com.chongcc.test.Result.ApiResponse;
import com.chongcc.test.Service.AI.AIPowerPointService;
import com.chongcc.test.Service.Calligraphy.CourseService;
import com.chongcc.test.dto.CourseMessage;
import com.chongcc.test.dto.DialHstResource;
import com.chongcc.test.dto.HstResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityExistsException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/course")
public class CourseController {
    private final CourseService courseService;
    private final AIPowerPointService aiPowerPointService;
    public CourseController(CourseService courseService, AIPowerPointService aiPowerPointService){
        this.courseService = courseService;
        this.aiPowerPointService = aiPowerPointService;
    }
    @GetMapping("/page")
    public ApiResponse<Page<CourseMessage>> getCoursesOnPage(
            @RequestParam(name = "page", defaultValue = "0")int pageNum,
            @RequestParam(name = "size", defaultValue = "10")int pageSize){
        try {
            Page<CourseMessage> courses = courseService.getAllCourses(pageNum, pageSize);
            return new ApiResponse<>(200, "success", courses);
        } catch (IllegalArgumentException e){
            return new ApiResponse<>(400, "invalid argument", null);
        } catch (Exception e){
            return new ApiResponse<>(503, "error", null);
        }
    }

    @PostMapping("/courses")
    public ApiResponse<?> createCourse(@RequestBody CourseMessage courseMessage){
        try{
            if (courseMessage.getCourseName() == null || courseMessage.getCourseName().trim().isEmpty()
                    || courseMessage.getCourseCategory() == null || courseMessage.getCourseCategory().trim().isEmpty()
                    || courseMessage.getCourseId() == null){
                return new ApiResponse<>(400, "missing field", null);
            }
            courseService.register(courseMessage);
            return new ApiResponse<>(200, "register success", null);
        } catch (EntityExistsException e){
            return new ApiResponse<>(409, "course already exists", null);
        }  catch (Exception e){
            return new ApiResponse<>(500, "server error", null);
        }
    }

    @PostMapping("/dial")
    public ApiResponse<?> addDialHst(@RequestParam int courseId, @RequestParam int dialId){
        try{
            courseService.addDialHst(courseId, dialId);
            return new ApiResponse<>(200, "add dial hst success", null);
        } catch (DuplicateKeyException e){
            return new ApiResponse<>(409, e.getMessage(), null);
        } catch (Exception e){
            return new ApiResponse<>(500, "server error", null);
        }
    }

    @DeleteMapping("/course")
    public ApiResponse<?> deleteCourse(@RequestParam int courseId){
        try{
            courseService.deleteCourse(courseId);
            return new ApiResponse<>(200, "delete course success", null);
        } catch (Exception e){
            return new ApiResponse<>(500, "server error", null);
        }
    }

    @GetMapping("/user/courses")
    public ApiResponse<?> getUserParticipatedCourses(@RequestParam int userId){
        try{
            List<Course> courses = courseService.getUserParticipatedCourses(userId);
            return new ApiResponse<>(200, "get user participated courses success", courses);
        } catch (RuntimeException e){
            return new ApiResponse<>(404, e.getMessage(), null);
        } catch (Exception e){
            return new ApiResponse<>(500, "server error", null);
        }
    }

    @GetMapping("/details")
    public ApiResponse<?> getCourseDetails(@RequestParam int courseId){
        try{
            CourseMessage courseDetails = courseService.getCourseDetails(courseId);
            return new ApiResponse<>(200, "get course details success", courseDetails);
        } catch (RuntimeException e){
            return new ApiResponse<>(404, e.getMessage(), null);
        } catch (Exception e){
            return new ApiResponse<>(500, "server error", null);
        }
    }
    @GetMapping("/powerpoint")
    public ApiResponse<?> getPowerPoints(@RequestParam int courseId){
        try {
            List<Integer> pptIds = courseService.getCoursePPTIds(courseId);
            List<DialHstResource> pptResources = new ArrayList<>();

            for (Integer dialId : pptIds) {
                HstResponse hstResponse = aiPowerPointService.findById(dialId);
                DialHstResource resource = new DialHstResource();
                resource.setId(hstResponse.getId());
                resource.setDialType(hstResponse.getDialType());
                resource.setQuestion(hstResponse.getQuestion());
                resource.setConversationId(hstResponse.getConversationId());
                resource.setChatId(hstResponse.getChatId());
                resource.setUserId(hstResponse.getUserId());

                // 获取详细信息，提取下载链接
                if (hstResponse.getConversationId() != null && hstResponse.getChatId() != null) {
                    ApiResponse<?> detailResponse = aiPowerPointService.getHstById(
                            hstResponse.getConversationId(),
                            hstResponse.getChatId()
                    );

                    if (detailResponse.getCode() == 200 && detailResponse.getData() != null) {
                        List<Map<String, Object>> detailData = (List<Map<String, Object>>) detailResponse.getData();
                        String downloadUrl = extractDownloadUrl(detailData);
                        resource.setDownloadUrl(downloadUrl);
                    }
                }

                pptResources.add(resource);
            }

            return new ApiResponse<>(200, "get power points success", pptResources);
        } catch (RuntimeException e){
            return new ApiResponse<>(404, e.getMessage(), null);
        } catch (Exception e){
            return new ApiResponse<>(500, "server error", null);
        }
    }

    private String extractDownloadUrl(List<Map<String, Object>> detailData) {
        for (Map<String, Object> item : detailData) {
            if ("text".equals(item.get("content_type"))) {
                String content = (String) item.get("content");
                if (content != null) {
                    try {
                        // 尝试解析JSON内容
                        ObjectMapper objectMapper = new ObjectMapper();
                        Map<String, Object> parsedContent = objectMapper.readValue(content, Map.class);

                        // 检查是否是PPT输出
                        if (parsedContent.containsKey("output")) {
                            String output = (String) parsedContent.get("output");
                            if (output != null && output.contains("http")) {
                                return output;
                            }
                        }
                    } catch (Exception e) {
                        // 如果不是JSON，检查是否包含下载链接
                        if (content.contains("http") && content.contains("files.islide.cc")) {
                            // 提取URL
                            int startIndex = content.indexOf("http");
                            int endIndex = content.indexOf(" ", startIndex);
                            if (endIndex == -1) {
                                endIndex = content.length();
                            }
                            return content.substring(startIndex, endIndex);
                        }
                    }
                }
            }
        }
        return null;
    }
}
