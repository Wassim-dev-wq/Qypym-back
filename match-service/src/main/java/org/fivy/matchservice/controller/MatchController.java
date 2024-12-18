package org.fivy.matchservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fivy.matchservice.dto.MatchRequestDTO;
import org.fivy.matchservice.dto.MatchResponseDTO;
import org.fivy.matchservice.service.MatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @PostMapping
    public ResponseEntity<MatchResponseDTO> createMatch(@Valid @RequestBody MatchRequestDTO matchRequestDTO) {
        MatchResponseDTO responseDTO = matchService.createMatch(matchRequestDTO);
        return ResponseEntity.status(201).body(responseDTO);
    }

    @GetMapping
    public ResponseEntity<List<MatchResponseDTO>> getAllMatches() {
        List<MatchResponseDTO> matchResponseDTOS = matchService.getAllMatches();
        return ResponseEntity.ok(matchResponseDTOS);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchResponseDTO> getMatchById(@PathVariable UUID id) {
        MatchResponseDTO matchResponseDTO = matchService.getMatchById(id);
        return ResponseEntity.ok(matchResponseDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MatchResponseDTO> updateMatch(
            @PathVariable UUID id,
            @Valid @RequestBody MatchRequestDTO matchRequestDTO) {
        MatchResponseDTO matchResponseDTO = matchService.updateMatch(id, matchRequestDTO);
        return ResponseEntity.ok(matchResponseDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMatch(@PathVariable UUID id) {
        matchService.deleteMatch(id);
        return ResponseEntity.noContent().build();
    }
}