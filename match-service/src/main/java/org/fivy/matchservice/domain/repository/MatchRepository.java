package org.fivy.matchservice.domain.repository;

import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.enums.MatchStatus;
import org.fivy.matchservice.domain.enums.PlayerStatus;
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

    @Query("SELECT m FROM Match m WHERE " +
            "m.startDate BETWEEN :now AND :upcomingThreshold " +
            "AND m.status IN (org.fivy.matchservice.domain.enums.MatchStatus.OPEN, " +
            "org.fivy.matchservice.domain.enums.MatchStatus.IN_PROGRESS) " +
            "AND (m.verificationCode IS NULL OR m.codeExpiryTime IS NULL OR m.codeExpiryTime < :now)")
    List<Match> findUpcomingMatchesNeedingCodes(
            @Param("now") ZonedDateTime now,
            @Param("upcomingThreshold") ZonedDateTime upcomingThreshold);


    @Query("SELECT m FROM Match m WHERE " +
            "m.startDate BETWEEN :startWindow AND :endWindow AND " +
            "m.status = :status")
    List<Match> findUpcomingMatchesInTimeWindow(
            @Param("startWindow") ZonedDateTime startWindow,
            @Param("endWindow") ZonedDateTime endWindow,
            @Param("status") MatchStatus status);


    @Query(value = "SELECT * FROM matches m " +
            "WHERE m.status = 'IN_PROGRESS' " +
            "OR m.status = 'OPEN' " +
            "AND (m.start_date + (m.duration * interval '1 minute')) < now()",
            nativeQuery = true)
    List<Match> findMatchesToFinish();

    @Query(value = """
        SELECT m.* FROM matches m
        WHERE (:searchQuery IS NULL OR 
               LOWER(m.title) LIKE LOWER(CONCAT('%', COALESCE(:searchQuery, ''), '%')))
        AND (:skillLevelsEmpty = true OR m.skill_level IN :skillLevels)
        AND (:statusesEmpty = true OR m.status IN :statuses)
        AND (:formatsEmpty = true OR m.format IN :formats)
        AND (
            :latitude IS NULL OR :longitude IS NULL OR :distance IS NULL
            OR
            (
                m.latitude IS NOT NULL AND m.longitude IS NOT NULL AND
                (6371 * acos(
                    GREATEST(
                        LEAST(
                            cos(CAST(:latitude AS double precision) * pi()/180) * 
                            cos(CAST(m.latitude AS double precision) * pi()/180) *
                            cos(CAST(m.longitude AS double precision) * pi()/180 - 
                              CAST(:longitude AS double precision) * pi()/180) +
                            sin(CAST(:latitude AS double precision) * pi()/180) * 
                            sin(CAST(m.latitude AS double precision) * pi()/180),
                            1.0
                        ),
                        -1.0
                    )
                )) <= :distance
            )
        )
        """,
            countQuery = """
        SELECT COUNT(*) FROM matches m
        WHERE (:searchQuery IS NULL OR 
               LOWER(m.title) LIKE LOWER(CONCAT('%', COALESCE(:searchQuery, ''), '%')))
        AND (:skillLevelsEmpty = true OR m.skill_level IN :skillLevels)
        AND (:statusesEmpty = true OR m.status IN :statuses)
        AND (:formatsEmpty = true OR m.format IN :formats)
        AND (
            :latitude IS NULL OR :longitude IS NULL OR :distance IS NULL
            OR
            (
                m.latitude IS NOT NULL AND m.longitude IS NOT NULL AND
                (6371 * acos(
                    GREATEST(
                        LEAST(
                            cos(CAST(:latitude AS double precision) * pi()/180) * 
                            cos(CAST(m.latitude AS double precision) * pi()/180) *
                            cos(CAST(m.longitude AS double precision) * pi()/180 - 
                              CAST(:longitude AS double precision) * pi()/180) +
                            sin(CAST(:latitude AS double precision) * pi()/180) * 
                            sin(CAST(m.latitude AS double precision) * pi()/180),
                            1.0
                        ),
                        -1.0
                    )
                )) <= :distance
            )
        )
        """,
            nativeQuery = true)
    Page<Match> findAllByFilters(
            @Param("searchQuery") String searchQuery,
            @Param("skillLevels") List<String> skillLevels,
            @Param("skillLevelsEmpty") boolean skillLevelsEmpty,
            @Param("statuses") List<String> statuses,
            @Param("statusesEmpty") boolean statusesEmpty,
            @Param("formats") List<String> formats,
            @Param("formatsEmpty") boolean formatsEmpty,
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("distance") Double distance,
            Pageable pageable
    );

    @Query("SELECT DISTINCT m FROM Match m " +
            "JOIN MatchPlayer mp ON mp.match = m " +
            "WHERE mp.playerId = :userId " +
            "AND mp.status != :excludePlayerStatus " +
            "AND m.startDate > :currentTime " +
            "AND m.status IN :statuses " +
            "ORDER BY m.startDate ASC")
    Page<Match> findUserUpcomingMatches(
            @Param("userId") UUID userId,
            @Param("currentTime") ZonedDateTime currentTime,
            @Param("statuses") List<MatchStatus> statuses,
            @Param("excludePlayerStatus") PlayerStatus excludePlayerStatus,
            Pageable pageable
    );
}