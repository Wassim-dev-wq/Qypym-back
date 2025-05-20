package org.fivy.matchservice.application.service.impl;

import org.fivy.matchservice.api.dto.response.MatchResponse;
import org.fivy.matchservice.api.mapper.MatchMapper;
import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.entity.SavedMatch;
import org.fivy.matchservice.domain.repository.MatchRepository;
import org.fivy.matchservice.domain.repository.SavedMatchRepository;
import org.fivy.matchservice.shared.exception.MatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MatchSavedServiceImplTest {

    @Mock
    private SavedMatchRepository savedMatchRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchMapper matchMapper;

    @InjectMocks
    private MatchSavedServiceImpl matchSavedService;

    @Captor
    private ArgumentCaptor<SavedMatch> savedMatchCaptor;

    private UUID matchId;
    private UUID userId;
    private UUID creatorId;
    private Match match;
    private SavedMatch savedMatch;
    private MatchResponse matchResponse;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        matchId = UUID.randomUUID();
        userId = UUID.randomUUID();
        creatorId = UUID.randomUUID();


        match = new Match();
        match.setId(matchId);
        match.setCreatorId(creatorId);


        savedMatch = SavedMatch.builder()
                .id(UUID.randomUUID())
                .match(match)
                .userId(userId)
                .savedAt(ZonedDateTime.now())
                .build();


        matchResponse = new MatchResponse();
        matchResponse.setId(matchId);
        matchResponse.setOwner(false);


        pageable = PageRequest.of(0, 10);
    }

    @Test
    void saveMatch_Success() {

        when(savedMatchRepository.existsByMatchIdAndUserId(matchId, userId)).thenReturn(false);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));


        matchSavedService.saveMatch(matchId, userId);


        verify(savedMatchRepository).existsByMatchIdAndUserId(matchId, userId);
        verify(matchRepository).findById(matchId);
        verify(savedMatchRepository).save(savedMatchCaptor.capture());

        SavedMatch capturedSavedMatch = savedMatchCaptor.getValue();
        assertEquals(match, capturedSavedMatch.getMatch());
        assertEquals(userId, capturedSavedMatch.getUserId());
        assertNotNull(capturedSavedMatch.getSavedAt());
    }

    @Test
    void saveMatch_AlreadySaved() {

        when(savedMatchRepository.existsByMatchIdAndUserId(matchId, userId)).thenReturn(true);


        MatchException exception = assertThrows(MatchException.class, () ->
                matchSavedService.saveMatch(matchId, userId));

        assertEquals("Match already saved", exception.getMessage());
        assertEquals("MATCH_ALREADY_SAVED", exception.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());

        verify(savedMatchRepository).existsByMatchIdAndUserId(matchId, userId);
        verify(matchRepository, never()).findById(any());
        verify(savedMatchRepository, never()).save(any());
    }

    @Test
    void saveMatch_MatchNotFound() {

        when(savedMatchRepository.existsByMatchIdAndUserId(matchId, userId)).thenReturn(false);
        when(matchRepository.findById(matchId)).thenReturn(Optional.empty());


        MatchException exception = assertThrows(MatchException.class, () ->
                matchSavedService.saveMatch(matchId, userId));

        assertTrue(exception.getMessage().contains("Match not found"));
        assertEquals("MATCH_NOT_FOUND", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(savedMatchRepository).existsByMatchIdAndUserId(matchId, userId);
        verify(matchRepository).findById(matchId);
        verify(savedMatchRepository, never()).save(any());
    }

    @Test
    void unsaveMatch_Success() {

        when(savedMatchRepository.findByMatchIdAndUserId(matchId, userId)).thenReturn(Optional.of(savedMatch));


        matchSavedService.unsaveMatch(matchId, userId);


        verify(savedMatchRepository).findByMatchIdAndUserId(matchId, userId);
        verify(savedMatchRepository).delete(savedMatch);
    }

    @Test
    void unsaveMatch_NotFound() {

        when(savedMatchRepository.findByMatchIdAndUserId(matchId, userId)).thenReturn(Optional.empty());


        MatchException exception = assertThrows(MatchException.class, () ->
                matchSavedService.unsaveMatch(matchId, userId));

        assertEquals("Match not found in saved matches", exception.getMessage());
        assertEquals("SAVED_MATCH_NOT_FOUND", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(savedMatchRepository).findByMatchIdAndUserId(matchId, userId);
        verify(savedMatchRepository, never()).delete(any());
    }

    @Test
    void getSavedMatches_Success() {

        List<SavedMatch> savedMatches = Arrays.asList(savedMatch);
        Page<SavedMatch> savedMatchesPage = new PageImpl<>(savedMatches, pageable, savedMatches.size());

        when(savedMatchRepository.findByUserId(userId, pageable)).thenReturn(savedMatchesPage);
        when(matchMapper.toMatchResponse(match)).thenReturn(matchResponse);


        Page<MatchResponse> result = matchSavedService.getSavedMatches(userId, pageable);


        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        MatchResponse responseItem = result.getContent().get(0);
        assertEquals(matchId, responseItem.getId());
        assertFalse(responseItem.isOwner());

        verify(savedMatchRepository).findByUserId(userId, pageable);
        verify(matchMapper).toMatchResponse(match);
    }

    @Test
    void getSavedMatches_WithOwnerMatch() {


        Match ownedMatch = new Match();
        ownedMatch.setId(UUID.randomUUID());
        ownedMatch.setCreatorId(userId);

        SavedMatch savedOwnedMatch = SavedMatch.builder()
                .id(UUID.randomUUID())
                .match(ownedMatch)
                .userId(userId)
                .savedAt(ZonedDateTime.now())
                .build();

        MatchResponse ownedMatchResponse = new MatchResponse();
        ownedMatchResponse.setId(ownedMatch.getId());

        List<SavedMatch> savedMatches = Arrays.asList(savedOwnedMatch);
        Page<SavedMatch> savedMatchesPage = new PageImpl<>(savedMatches, pageable, savedMatches.size());

        when(savedMatchRepository.findByUserId(userId, pageable)).thenReturn(savedMatchesPage);
        when(matchMapper.toMatchResponse(ownedMatch)).thenReturn(ownedMatchResponse);


        Page<MatchResponse> result = matchSavedService.getSavedMatches(userId, pageable);


        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        MatchResponse responseItem = result.getContent().get(0);
        assertEquals(ownedMatch.getId(), responseItem.getId());
        assertTrue(responseItem.isOwner());

        verify(savedMatchRepository).findByUserId(userId, pageable);
        verify(matchMapper).toMatchResponse(ownedMatch);
    }

    @Test
    void getSavedMatches_EmptyPage() {

        Page<SavedMatch> emptyPage = new PageImpl<>(Arrays.asList(), pageable, 0);
        when(savedMatchRepository.findByUserId(userId, pageable)).thenReturn(emptyPage);


        Page<MatchResponse> result = matchSavedService.getSavedMatches(userId, pageable);


        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());

        verify(savedMatchRepository).findByUserId(userId, pageable);
        verify(matchMapper, never()).toMatchResponse(any());
    }

    @Test
    void isMatchSaved_True() {

        when(savedMatchRepository.existsByMatchIdAndUserId(matchId, userId)).thenReturn(true);


        boolean result = matchSavedService.isMatchSaved(matchId, userId);


        assertTrue(result);
        verify(savedMatchRepository).existsByMatchIdAndUserId(matchId, userId);
    }

    @Test
    void isMatchSaved_False() {

        when(savedMatchRepository.existsByMatchIdAndUserId(matchId, userId)).thenReturn(false);


        boolean result = matchSavedService.isMatchSaved(matchId, userId);


        assertFalse(result);
        verify(savedMatchRepository).existsByMatchIdAndUserId(matchId, userId);
    }
}