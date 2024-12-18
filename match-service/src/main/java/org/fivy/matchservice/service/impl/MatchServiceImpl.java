package org.fivy.matchservice.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.fivy.matchservice.dto.MatchRequestDTO;
import org.fivy.matchservice.dto.MatchResponseDTO;
import org.fivy.matchservice.entity.Match;
import org.fivy.matchservice.entity.MatchRequiredPosition;
import org.fivy.matchservice.repository.MatchRepository;
import org.fivy.matchservice.repository.MatchRequiredPositionRepository;
import org.fivy.matchservice.service.MatchService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private final MatchRepository matchRepository;
    private final MatchRequiredPositionRepository matchRequiredPositionRepository;
    private final ModelMapper modelMapper;

    @Override
    public MatchResponseDTO createMatch(MatchRequestDTO matchRequestDTO) {
        Match match = modelMapper.map(matchRequestDTO, Match.class);
        Match savedMatch = matchRepository.save(match);
        List<MatchRequiredPosition> requiredPositions = matchRequestDTO.getRequiredPositions()
                .stream()
                .map(dto -> MatchRequiredPosition.builder()
                        .matchId(savedMatch.getId())
                        .positionId(dto.getPositionId())
                        .quantity(dto.getQuantity())
                        .build())
                .collect(Collectors.toList());
        matchRequiredPositionRepository.saveAll(requiredPositions);
        MatchResponseDTO responseDTO = modelMapper.map(savedMatch, MatchResponseDTO.class);
        responseDTO.setRequiredPositions(matchRequestDTO.getRequiredPositions());
        return responseDTO;
    }

    @Override
    public MatchResponseDTO getMatchById(UUID matchId) {
        return modelMapper.map(matchRepository.findById(matchId)
                        .orElseThrow(() -> new EntityNotFoundException("Match not found with UUID: " + matchId)),
                MatchResponseDTO.class);
    }

    @Override
    public List<MatchResponseDTO> getAllMatches() {
        return matchRepository.findAll()
                .stream()
                .map((match) -> modelMapper.map(match, MatchResponseDTO.class))
                .collect(Collectors.toList());
    }

    // TODO : implement the update logic
    @Override
    public MatchResponseDTO updateMatch(UUID matchId, MatchRequestDTO matchRequestDTO) {
        Match existingMatch = matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match not found with ID: " + matchId));
        return null;
    }

    @Override
    public void deleteMatch(UUID matchId) {
        matchRepository.findById(matchId)
                .ifPresentOrElse(
                        matchRepository::delete,
                        () -> new EntityNotFoundException("Match not found with UUID: " + matchId));
    }
}
