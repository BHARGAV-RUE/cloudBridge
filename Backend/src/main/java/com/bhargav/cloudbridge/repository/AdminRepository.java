package com.bhargav.cloudbridge.repository;

import com.bhargav.cloudbridge.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, String> {

    Optional<Admin> findByUsername(String username);

    boolean existsByUsername(String username);
}