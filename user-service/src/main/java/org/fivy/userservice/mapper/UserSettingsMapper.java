package org.fivy.userservice.mapper;

import org.fivy.userservice.dto.UserSettingsDto;
import org.fivy.userservice.entity.UserSettings;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserSettingsMapper {

    @Mappings({
            @Mapping(source = "notificationsEnabled",target = "notificationsEnabled"),
            @Mapping(source = "emailNotificationsEnabled",target = "emailNotificationsEnabled"),
            @Mapping(source = "pushNotificationEnabled",target = "pushNotificationEnabled"),
            @Mapping(source = "privacyVisibility",target = "privacyVisibility"),
            @Mapping(target = "createdAt", source = "createdAt"),
            @Mapping(target = "updatedAt", source = "updatedAt")
    })
    UserSettingsDto toDto(UserSettings userSettings);

    @Mappings({
            @Mapping(source = "notificationsEnabled",target = "notificationsEnabled"),
            @Mapping(source = "emailNotificationsEnabled",target = "emailNotificationsEnabled"),
            @Mapping(source = "pushNotificationEnabled",target = "pushNotificationEnabled"),
            @Mapping(source = "privacyVisibility",target = "privacyVisibility"),
    })
    UserSettings toEntity(UserSettingsDto userSettingsDto);
}
