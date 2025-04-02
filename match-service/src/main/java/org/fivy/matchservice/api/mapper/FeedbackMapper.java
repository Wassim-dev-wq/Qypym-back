package org.fivy.matchservice.api.mapper;

import org.fivy.matchservice.api.dto.request.SubmitMatchFeedbackRequest;
import org.fivy.matchservice.api.dto.response.FeedbackRequestResponse;
import org.fivy.matchservice.api.dto.response.PlayerRatingSummaryResponse;
import org.fivy.matchservice.domain.entity.MatchFeedbackRequest;
import org.fivy.matchservice.domain.entity.PlayerMatchFeedback;
import org.fivy.matchservice.domain.entity.PlayerRating;
import org.fivy.matchservice.domain.entity.PlayerRatingSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface FeedbackMapper {

    @Mapping(target = "matchId", source = "match.id")
    @Mapping(target = "matchTitle", source = "match.title")
    @Mapping(target = "feedbackCount", ignore = true)
    @Mapping(target = "totalPlayersInMatch", ignore = true)
    @Mapping(target = "userHasSubmitted", ignore = true)
    FeedbackRequestResponse toFeedbackRequestResponse(MatchFeedbackRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "submittedAt", expression = "java(java.time.ZonedDateTime.now())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "playerRatings", ignore = true)
    void updateFeedbackFromDto(SubmitMatchFeedbackRequest dto, @MappingTarget PlayerMatchFeedback feedback);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "feedback", ignore = true)
    @Mapping(target = "ratingPlayerId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    PlayerRating toPlayerRatingEntity(SubmitMatchFeedbackRequest.PlayerRatingRequest dto);


    @Mapping(target = "skillRating", source = "avgSkillRating")
    @Mapping(target = "sportsmanshipRating", source = "avgSportsmanshipRating")
    @Mapping(target = "teamworkRating", source = "avgTeamworkRating")
    @Mapping(target = "reliabilityRating", source = "avgReliabilityRating")
    PlayerRatingSummaryResponse toPlayerRatingSummaryResponse(PlayerRatingSummary summary);
}