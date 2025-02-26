package org.fivy.matchservice.api.mapper;

import org.fivy.matchservice.api.dto.request.CreateMatchRequest;
import org.fivy.matchservice.api.dto.request.UpdateMatchRequest;
import org.fivy.matchservice.api.dto.response.MatchDetailsResponse;
import org.fivy.matchservice.api.dto.response.MatchResponse;
import org.fivy.matchservice.domain.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface MatchMapper {

    @Mapping(target = "skillLevel", source = "skillLevel")
    @Mapping(target = "format", source = "format")
    MatchResponse toMatchResponse(Match match);

    @Mapping(target = "skillLevel", source = "skillLevel")
    @Mapping(target = "format", source = "format")
    @Mapping(target = "status", constant = "DRAFT")
    Match toEntity(CreateMatchRequest request);
    void updateMatchFromDto(UpdateMatchRequest request, @MappingTarget Match match);

    @Mapping(target = "coordinates", source = "coordinates")
    Location toEntity(CreateMatchRequest.LocationDTO locationDTO);

    Coordinates toEntity(CreateMatchRequest.CoordinatesDTO coordinatesDTO);

    @Mapping(target = "isOwner", ignore = true)
    @Mapping(target = "saved", ignore = true)
    @Mapping(target = "savedCount", ignore = true)
    @Mapping(target = "players", ignore = true)
    @Mapping(target = "teams", ignore = true)
    @Mapping(target = "currentPlayers", ignore = true)
    MatchDetailsResponse toDetailedMatchResponse(Match match);

    @Mapping(target = "coordinates", source = "coordinates")
    MatchResponse.LocationDTO toLocationDTO(Location location);

    MatchDetailsResponse.CoordinatesDTO toCoordinatesDTO(Coordinates coordinates);

    @Mapping(target = "team", source = "team", qualifiedByName = "toTeamInfo")
    MatchDetailsResponse.MatchPlayerResponse toDetailedPlayerResponse(MatchPlayer player);

    @Named("toTeamInfo")
    default MatchDetailsResponse.MatchPlayerResponse.TeamInfo toTeamInfo(MatchTeam team) {
        if (team == null) {
            return null;
        }
        return MatchDetailsResponse.MatchPlayerResponse.TeamInfo.builder()
                .id(team.getId())
                .name(team.getName())
                .teamNumber(team.getTeamNumber())
                .build();
    }

    @Mapping(target = "currentPlayers", ignore = true)
    @Mapping(target = "players", ignore = true)
    MatchDetailsResponse.MatchTeamResponse toDetailedTeamResponse(MatchTeam team);

    @Mapping(target = "temperature", source = "temperature")
    @Mapping(target = "condition", source = "condition")
    @Mapping(target = "humidity", source = "humidity")
    @Mapping(target = "windSpeed", source = "windSpeed")
    @Mapping(target = "cloudCoverage", source = "cloudCoverage")
    @Mapping(target = "weatherId", source = "weatherId")
    MatchDetailsResponse.WeatherResponse toWeatherResponse(MatchWeather matchWeather);

}
