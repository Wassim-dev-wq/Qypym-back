package org.fivy.matchservice.domain.repository;

import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.enums.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID>, JpaSpecificationExecutor<Match> {

    Page<Match> findByCreatorId(UUID creatorId, Pageable pageable);


    List<Match> findByStatusAndStartDateBetween(MatchStatus status, ZonedDateTime startDate, ZonedDateTime endDate);

}