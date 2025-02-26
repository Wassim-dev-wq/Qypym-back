package org.fivy.matchservice.domain.repository;

import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.enums.MatchStatus;
import org.fivy.matchservice.domain.enums.SkillLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID>, JpaSpecificationExecutor<Match> {

    Page<Match> findByCreatorId(UUID creatorId, Pageable pageable);

    List<Match> findByStatusAndStartDateBetween(MatchStatus status, ZonedDateTime startDate, ZonedDateTime endDate);

    @Query("""
            SELECT m
            FROM Match m
            WHERE (6371 *
              ACOS(
                COS(RADIANS(:latitude)) * COS(RADIANS(m.location.coordinates.latitude)) *
                COS(RADIANS(m.location.coordinates.longitude) - RADIANS(:longitude)) +
                SIN(RADIANS(:latitude)) * SIN(RADIANS(m.location.coordinates.latitude))
              )
            ) <= :distance
           """)
    Page<Match> findAllWithinDistance(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("distance") Double distance,
            Pageable pageable
    );


    @Query("""
            SELECT m
            FROM Match m
            WHERE (6371 *
              ACOS(
                COS(RADIANS(:latitude)) * COS(RADIANS(m.location.coordinates.latitude)) *
                COS(RADIANS(m.location.coordinates.longitude) - RADIANS(:longitude)) +
                SIN(RADIANS(:latitude)) * SIN(RADIANS(m.location.coordinates.latitude))
              )
            ) <= :distance
              AND m.skillLevel = :skillLevel
           """)
    Page<Match> findAllWithinDistanceAndSkill(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("distance") Double distance,
            @Param("skillLevel") SkillLevel skillLevel,
            Pageable pageable
    );


    Page<Match> findAllBySkillLevel(SkillLevel skillLevel, Pageable pageable);

    Page<Match> findAll(Pageable pageable);

    @Query("""
    SELECT DISTINCT m FROM Match m
    LEFT JOIN FETCH m.teams t
    LEFT JOIN FETCH m.players p
    LEFT JOIN FETCH p.team
    WHERE m.id = :matchId
    """)
    Optional<Match> findMatchWithDetails(@Param("matchId") UUID matchId);
}