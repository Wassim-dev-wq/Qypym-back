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

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
        if (match.getStartDate() == null) {
            log.warn("Match has no scheduled time. Falling back to current weather.");
            return fetchAndSaveCurrentWeather(match);
        }
        final double lat = match.getLocation().getCoordinates().getLatitude();
        final double lon = match.getLocation().getCoordinates().getLongitude();
        final ZonedDateTime matchTime = match.getStartDate();
        try {
            RestTemplate restTemplate = new RestTemplate();
            return fetchAndSaveForecastWeather(match, lat, lon, matchTime, restTemplate);
        } catch (RestClientException ex) {
            log.error("Failed to fetch weather data for match {}: {}", match.getId(), ex.getMessage());
            return null;
        }
    }

    private MatchWeather fetchAndSaveCurrentWeather(Match match) {
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
            return saveWeatherFromResponse(match, response);
        } catch (RestClientException ex) {
            log.error("Failed to fetch current weather data: {}", ex.getMessage());
            return null;
        }
    }

    private MatchWeather fetchAndSaveForecastWeather(Match match, double lat, double lon,
                                                     ZonedDateTime matchTime, RestTemplate restTemplate) {
        String url = String.format(
                "https://api.openweathermap.org/data/2.5/forecast?lat=%f&lon=%f&units=metric&appid=%s",
                lat, lon, openWeatherApiKey
        );
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response == null) {
            log.error("Invalid forecast weather response for match: {}", match.getId());
            return null;
        }
        List<Map<String, Object>> forecasts = (List<Map<String, Object>>) response.get("list");
        if (forecasts == null || forecasts.isEmpty()) {
            log.error("No forecast data available for match: {}", match.getId());
            return null;
        }
        Optional<Map<String, Object>> closestForecast = findClosestWeatherData(forecasts, matchTime);
        if (closestForecast.isPresent()) {
            Map<String, Object> weatherData = closestForecast.get();
            return saveWeatherFromForecastResponse(match, weatherData);
        } else {
            return fetchAndSaveCurrentWeather(match);
        }
    }

    private Optional<Map<String, Object>> findClosestWeatherData(List<Map<String, Object>> weatherDataList,
                                                                 ZonedDateTime matchTime) {
        return weatherDataList.stream()
                .filter(data -> {
                    long dataTimestamp;
                    if (data.containsKey("dt")) {
                        dataTimestamp = ((Number) data.get("dt")).longValue();
                    } else {
                        return false;
                    }
                    ZonedDateTime dataTime = Instant.ofEpochSecond(dataTimestamp)
                            .atZone(matchTime.getZone());
                    long hoursDifference = Math.abs(ChronoUnit.HOURS.between(dataTime, matchTime));
                    return hoursDifference <= 1;
                })
                .min((data1, data2) -> {
                    long timestamp1 = ((Number) data1.get("dt")).longValue();
                    long timestamp2 = ((Number) data2.get("dt")).longValue();
                    ZonedDateTime time1 = Instant.ofEpochSecond(timestamp1).atZone(matchTime.getZone());
                    ZonedDateTime time2 = Instant.ofEpochSecond(timestamp2).atZone(matchTime.getZone());
                    long diff1 = Math.abs(Duration.between(time1, matchTime).toMinutes());
                    long diff2 = Math.abs(Duration.between(time2, matchTime).toMinutes());
                    return Long.compare(diff1, diff2);
                });
    }

    private MatchWeather saveWeatherFromResponse(Match match, Map<String, Object> response) {
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
    }

    private MatchWeather saveWeatherFromForecastResponse(Match match, Map<String, Object> forecastData) {
        Map<String, Object> main = (Map<String, Object>) forecastData.get("main");
        List<Map<String, Object>> weather = (List<Map<String, Object>>) forecastData.get("weather");
        Map<String, Object> wind = (Map<String, Object>) forecastData.get("wind");
        Map<String, Object> clouds = (Map<String, Object>) forecastData.get("clouds");

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
    }

    public void deleteWeatherByMatchId(UUID matchId) {
    }
}