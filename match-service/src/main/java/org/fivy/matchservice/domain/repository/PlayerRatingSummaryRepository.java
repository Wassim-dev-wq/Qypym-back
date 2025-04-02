package org.fivy.matchservice.domain.repository;

import org.fivy.matchservice.domain.entity.PlayerRatingSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlayerRatingSummaryRepository extends JpaRepository<PlayerRatingSummary, UUID> {

    List<PlayerRatingSummary> findTop10ByOrderByOverallRatingDesc();
}