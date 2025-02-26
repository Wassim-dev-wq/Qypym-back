package org.fivy.matchservice.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.entity.MatchWeather;
import org.fivy.matchservice.domain.repository.MatchWeatherRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherServiceImpl {

    private final MatchWeatherRepository matchWeatherRepository;

    @Value("${openweather.api.key}")
    private String openWeatherApiKey;

    public MatchWeather fetchAndSaveWeather(Match match) {
        if (match.getLocation() == null || match.getLocation().getCoordinates() == null) {
            log.warn("Match has no valid location. Skipping weather save.");
            return null;
        }

        final double lat = match.getLocation().getCoordinates().getLatitude();
        final double lon = match.getLocation().getCoordinates().getLongitude();

        try {
            RestTemplate restTemplate = new RestTemplate();

            String url = String.format(
                    "https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&units=metric&appid=%s",
                    lat, lon, openWeatherApiKey
            );

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                log.error("Invalid weather response for match: {}", match.getId());
                return null;
            }

            Map<String, Object> main = (Map<String, Object>) response.get("main");
            List<Map<String, Object>> weather = (List<Map<String, Object>>) response.get("weather");
            Map<String, Object> wind = (Map<String, Object>) response.get("wind");
            Map<String, Object> clouds = (Map<String, Object>) response.get("clouds");

            Integer weatherId = weather != null && !weather.isEmpty() ?
                    ((Number) weather.get(0).get("id")).intValue() : null;
            String condition = weather != null && !weather.isEmpty() ?
                    (String) weather.get(0).get("main") : "Unknown";
            Integer temperature = (int) Math.round(((Number) main.getOrDefault("temp", 0)).doubleValue());
            Integer humidity = ((Number) main.getOrDefault("humidity", 0)).intValue();
            Integer windSpeed = (int) Math.round(((Number) wind.getOrDefault("speed", 0)).doubleValue() * 3.6);

            Integer cloudCoverage = ((Number) clouds.getOrDefault("all", 0)).intValue();

            MatchWeather matchWeather = MatchWeather.builder()
                    .match(match)
                    .temperature(temperature)
                    .condition(condition)
                    .humidity(humidity)
                    .windSpeed(windSpeed)
                    .weatherId(weatherId)
                    .cloudCoverage(cloudCoverage)
                    .build();

            return matchWeatherRepository.save(matchWeather);

        } catch (RestClientException ex) {
            log.error("Failed to fetch weather data: {}", ex.getMessage());
            return null;
        }
    }
}