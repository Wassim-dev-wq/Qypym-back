package org.fivy.matchservice.api.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fivy.matchservice.domain.enums.MatchFormat;
import org.fivy.matchservice.domain.enums.SkillLevel;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMatchRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private ZonedDateTime startDate;

    @NotNull(message = "Duration is required")
    @Min(value = 30, message = "Duration must be at least 30 minutes")
    @Max(value = 240, message = "Duration must not exceed 240 minutes")
    private Integer duration;

    @NotNull(message = "Format is required")
    private MatchFormat format;

    @NotNull(message = "Location is required")
    @Valid
    private LocationDTO location;

    @NotNull(message = "Skill level is required")
    private SkillLevel skillLevel;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be >= 0.0")
    @DecimalMax(value = "200.0", message = "Price must be <= 200.0")
    private Double price;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationDTO {

        @NotBlank(message = "Address is required")
        private String address;

        @NotNull(message = "Coordinates are required")
        @Valid
        private CoordinatesDTO coordinates;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CoordinatesDTO {

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
        private Double latitude;

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
        private Double longitude;
    }
}
