package org.fivy.matchservice.service;

import org.fivy.matchservice.dto.MatchRequestDTO;
import org.fivy.matchservice.dto.MatchResponseDTO;

import java.util.List;
import java.util.UUID;

public interface MatchService {
    MatchResponseDTO createMatch(MatchRequestDTO matchRequestDTO);

    MatchResponseDTO getMatchById(UUID matchId);

    List<MatchResponseDTO> getAllMatches();

    MatchResponseDTO updateMatch(UUID matchId, MatchRequestDTO matchRequestDTO);

    void deleteMatch(UUID matchId);
}
