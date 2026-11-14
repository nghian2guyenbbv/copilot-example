package com.example.demo.incident.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceMetricRepository extends JpaRepository<ServiceMetric, Long> {
    List<ServiceMetric> findTop10ByServiceNameOrderByIdDesc(String serviceName);

    Optional<ServiceMetric> findFirstByServiceNameOrderByIdDesc(String serviceName);
}
