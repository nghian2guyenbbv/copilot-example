package com.example.demo.incident.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findTop20ByStatusOrderByIdDesc(String status);
}
