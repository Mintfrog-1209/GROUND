package com.ground.domain.user.repository;

import java.util.List;
import java.util.Optional;

import com.ground.domain.user.dto.UserCategoryDto;
import com.ground.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ground.domain.user.entity.UserCategory;

public interface UserCategoryRepository  extends JpaRepository<UserCategory, Long>{
    List<UserCategory> findAllByUser(User user);

    void deleteAllByUser(User user);
}
