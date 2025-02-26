package org.fivy.matchservice.domain.repository;

import org.fivy.matchservice.domain.entity.MatchWeather;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MatchWeatherRepository extends JpaRepository<MatchWeather, UUID> {
}
