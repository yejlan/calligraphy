package com.chongcc.test.dao;

import com.chongcc.test.Entity.ImgHst;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImgHstRepo extends JpaRepository<ImgHst, Integer> {
    List<ImgHst> findByUserId(Integer userId);

    ImgHst findImgHstById(Integer Id);
}
