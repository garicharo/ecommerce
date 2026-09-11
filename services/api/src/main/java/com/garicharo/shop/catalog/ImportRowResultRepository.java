package com.garicharo.shop.catalog;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportRowResultRepository extends JpaRepository<ImportRowResult, UUID> {

    Page<ImportRowResult> findByJobId(UUID jobId, Pageable pageable);

    Page<ImportRowResult> findByJobIdAndOutcome(UUID jobId, ImportRowOutcome outcome, Pageable pageable);
}
