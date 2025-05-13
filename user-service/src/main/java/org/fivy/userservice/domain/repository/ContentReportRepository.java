package org.fivy.userservice.domain.repository;

import org.fivy.userservice.domain.entity.ContentReport;
import org.fivy.userservice.domain.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContentReportRepository extends JpaRepository<ContentReport, UUID> {
    List<ContentReport> findByReporterId(UUID reporterId);
    Page<ContentReport> findByReporterId(UUID reporterId, Pageable pageable);
    Page<ContentReport> findByStatus(ReportStatus status, Pageable pageable);
}