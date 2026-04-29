package com.chongcc.test.dao;

import com.chongcc.test.Entity.DialHst;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DialHstRepo extends JpaRepository<DialHst, Integer> {
    List<DialHst> findByUserIdAndDialType(Integer userId, String dialType);
}
