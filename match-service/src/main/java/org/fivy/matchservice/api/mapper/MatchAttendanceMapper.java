package org.fivy.matchservice.api.mapper;

import org.fivy.matchservice.api.dto.response.MatchAttendanceResponse;
import org.fivy.matchservice.domain.entity.MatchAttendance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MatchAttendanceMapper {

    @Mapping(source = "match.id", target = "matchId")
    MatchAttendanceResponse toAttendanceResponse(MatchAttendance attendance);

    List<MatchAttendanceResponse> toAttendanceResponseList(List<MatchAttendance> attendances);
}