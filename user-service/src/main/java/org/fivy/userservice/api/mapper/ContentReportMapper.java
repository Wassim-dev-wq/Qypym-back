package org.fivy.userservice.api.mapper;

import org.fivy.userservice.api.dto.ContentReportDTO;
import org.fivy.userservice.domain.entity.ContentReport;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ContentReportMapper {
    ContentReportDTO toContentReportDTO(ContentReport contentReport);
    ContentReport toContentReport(ContentReportDTO contentReportDTO);
}