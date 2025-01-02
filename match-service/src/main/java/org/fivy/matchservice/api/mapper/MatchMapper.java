package org.fivy.matchservice.api.mapper;

import org.fivy.matchservice.api.dto.request.CreateMatchRequest;
import org.fivy.matchservice.api.dto.request.UpdateMatchRequest;
import org.fivy.matchservice.api.dto.response.MatchResponse;
import org.fivy.matchservice.domain.entity.Coordinates;
import org.fivy.matchservice.domain.entity.Location;
import org.fivy.matchservice.domain.entity.Match;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MatchMapper {

    @Mapping(target = "skillLevel", source = "skillLevel")
    MatchResponse toMatchResponse(Match match);

    @Mapping(target = "skillLevel", source = "skillLevel")
    @Mapping(target = "status", constant = "DRAFT")
    Match toEntity(CreateMatchRequest request);
    void updateMatchFromDto(UpdateMatchRequest request, @MappingTarget Match match);

    @Mapping(target = "coordinates", source = "coordinates")
    Location toEntity(CreateMatchRequest.LocationDTO locationDTO);

    Coordinates toEntity(CreateMatchRequest.CoordinatesDTO coordinatesDTO);

}
