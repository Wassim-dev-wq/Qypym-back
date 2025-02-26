package org.fivy.matchservice.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.api.dto.response.MatchResponse;
import org.fivy.matchservice.api.mapper.MatchMapper;
import org.fivy.matchservice.application.service.MatchSavedService;
import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.entity.SavedMatch;
import org.fivy.matchservice.domain.repository.MatchRepository;
import org.fivy.matchservice.domain.repository.SavedMatchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MatchSavedServiceImpl implements MatchSavedService {
    private final SavedMatchRepository savedMatchRepository;
    private final MatchRepository matchRepository;
    private final MatchMapper matchMapper;

    @Override
    public void saveMatch(UUID matchId, UUID userId) {
        log.debug("Saving match: {} for user: {}", matchId, userId);

        if (savedMatchRepository.existsByMatchIdAndUserId(matchId, userId)) {
            throw new org.fivy.matchservice.shared.exception.MatchException("Match already saved", "MATCH_ALREADY_SAVED", HttpStatus.CONFLICT);
        }

        Match match = findMatchOrThrow(matchId);

        SavedMatch savedMatch = SavedMatch.builder()
                .match(match)
                .userId(userId)
                .savedAt(ZonedDateTime.now())
                .build();

        savedMatchRepository.save(savedMatch);
        log.debug("Match saved successfully");
    }

    @Override
    public void unsaveMatch(UUID matchId, UUID userId) {
        log.debug("Unsaving match: {} for user: {}", matchId, userId);

        SavedMatch savedMatch = savedMatchRepository.findByMatchIdAndUserId(matchId, userId)
                .orElseThrow(() -> new org.fivy.matchservice.shared.exception.MatchException(
                        "Match not found in saved matches",
                        "SAVED_MATCH_NOT_FOUND",
                        HttpStatus.NOT_FOUND
                ));

        savedMatchRepository.delete(savedMatch);
        log.debug("Match unsaved successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MatchResponse> getSavedMatches(UUID userId, Pageable pageable) {
        log.debug("Fetching saved matches for user: {}", userId);

        return savedMatchRepository.findByUserId(userId, pageable)
                .map(savedMatch -> {
                    MatchResponse response = matchMapper.toMatchResponse(savedMatch.getMatch());
                    response.setOwner(savedMatch.getMatch().getCreatorId().equals(userId));
                    return response;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMatchSaved(UUID matchId, UUID userId) {
        log.debug("Checking if match: {} is saved for user: {}", matchId, userId);
        return savedMatchRepository.existsByMatchIdAndUserId(matchId, userId);
    }

    private Match findMatchOrThrow(UUID matchId) {
        return matchRepository.findById(matchId).orElseThrow(() -> new org.fivy.matchservice.shared.exception.MatchException("Match not found with ID: " + matchId, "MATCH_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
