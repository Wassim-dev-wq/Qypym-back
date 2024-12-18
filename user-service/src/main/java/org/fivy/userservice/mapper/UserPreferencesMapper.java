package org.fivy.userservice.mapper;

import org.fivy.userservice.dto.UserPreferencesDTO;
import org.fivy.userservice.entity.UserPreferences;
import org.mapstruct.*;

@Mapper(componentModel = "spring",nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserPreferencesMapper {

    @Mappings({
            @Mapping(source = "preferredGenders",target = "preferredGenders"),
            @Mapping(source = "ageRangeMin",target = "ageRangeMin"),
            @Mapping(source = "ageRangeMax",target = "ageRangeMax"),
            @Mapping(source = "distanceRange",target = "distanceRange"),
            @Mapping(source = "interests",target = "interests"),
            @Mapping(source = "lifestylePreferences",target = "lifestylePreferences"),
            @Mapping(source = "educationLevel",target = "educationLevel"),
            @Mapping(source = "occupation",target = "occupation"),
            @Mapping(source = "languagePreferences",target = "languagePreferences"),
    })
    UserPreferencesDTO toUserPreferencesDTO(UserPreferences preferences);

    @Mappings({
            @Mapping(source = "preferredGenders",target = "preferredGenders"),
            @Mapping(source = "ageRangeMin",target = "ageRangeMin"),
            @Mapping(source = "ageRangeMax",target = "ageRangeMax"),
            @Mapping(source = "distanceRange",target = "distanceRange"),
            @Mapping(source = "interests",target = "interests"),
            @Mapping(source = "lifestylePreferences",target = "lifestylePreferences"),
            @Mapping(source = "educationLevel",target = "educationLevel"),
            @Mapping(source = "occupation",target = "occupation"),
            @Mapping(source = "languagePreferences",target = "languagePreferences"),
    })
    UserPreferences toUserPreferences(UserPreferencesDTO preferencesDTO);

    @Mappings({
            @Mapping(source = "preferredGenders",target = "preferredGenders"),
            @Mapping(source = "ageRangeMin",target = "ageRangeMin"),
            @Mapping(source = "ageRangeMax",target = "ageRangeMax"),
            @Mapping(source = "distanceRange",target = "distanceRange"),
            @Mapping(source = "interests",target = "interests"),
            @Mapping(source = "lifestylePreferences",target = "lifestylePreferences"),
            @Mapping(source = "educationLevel",target = "educationLevel"),
            @Mapping(source = "occupation",target = "occupation"),
            @Mapping(source = "languagePreferences",target = "languagePreferences"),
    })
    void updatePreferencesFromDto(UserPreferencesDTO dto, @MappingTarget UserPreferences entity);
}