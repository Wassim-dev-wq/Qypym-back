package org.fivy.matchservice.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fivy.matchservice.domain.enums.MatchFormat;
import org.fivy.matchservice.domain.enums.MatchStatus;
import org.fivy.matchservice.domain.enums.SkillLevel;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class FilterMatchesRequest {

    private String searchQuery;

    private Double latitude;

    private Double longitude;

    @Min(value = 1)
    @Max(value = 1000)
    private Double distance;

    private List<SkillLevel> skillLevels;

    private List<MatchStatus> statuses;

    private List<MatchFormat> formats;

    public List<SkillLevel> getSkillLevels() {
        return skillLevels == null ? new ArrayList<>() : skillLevels;
    }

    public List<MatchStatus> getStatuses() {
        return statuses == null ? new ArrayList<>() : statuses;
    }

    public List<MatchFormat> getFormats() {
        return formats == null ? new ArrayList<>() : formats;
    }


    public boolean hasLocationParameters() {
        return latitude != null && longitude != null && distance != null;
    }
}
