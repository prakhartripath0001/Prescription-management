package com.prescription_Management_REST_API.repository;

import com.prescription_Management_REST_API.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {}
