package com.example.demo.incident.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RunbookRepository extends JpaRepository<Runbook, Long> {
    List<Runbook> findByServiceName(String serviceName);
}
