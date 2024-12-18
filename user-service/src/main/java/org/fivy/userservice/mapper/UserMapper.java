package org.fivy.userservice.mapper;

import org.fivy.userservice.dto.UserRequestDTO;
import org.fivy.userservice.dto.UserResponseDTO;
import org.fivy.userservice.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    @Mappings({
            @Mapping(source = "username", target = "username"),
            @Mapping(source = "email", target = "email"),
            @Mapping(source = "firstName", target = "firstName"),
            @Mapping(source = "lastName", target = "lastName"),
            @Mapping(source = "phoneNumber", target = "phoneNumber"),
            @Mapping(source = "playerLevel", target = "playerLevel"),
            @Mapping(source = "dateOfBirth", target = "dateOfBirth"),
            @Mapping(source = "genderIdentity", target = "genderIdentity"),
            @Mapping(source = "bio", target = "bio"),
            @Mapping(source = "videoIntroUrl", target = "videoIntroUrl"),
            @Mapping(source = "latitude", target = "latitude"),
            @Mapping(source = "longitude", target = "longitude"),
            @Mapping(source = "preferredDistance", target = "preferredDistance")
    })
    User toUser(UserRequestDTO userRequestDTO);

    @Mappings({
            @Mapping(source = "username", target = "username"),
            @Mapping(source = "email", target = "email"),
            @Mapping(source = "firstName", target = "firstName"),
            @Mapping(source = "lastName", target = "lastName"),
            @Mapping(source = "phoneNumber", target = "phoneNumber"),
            @Mapping(source = "playerLevel", target = "playerLevel"),
            @Mapping(source = "dateOfBirth", target = "dateOfBirth"),
            @Mapping(source = "genderIdentity", target = "genderIdentity"),
            @Mapping(source = "bio", target = "bio"),
            @Mapping(source = "videoIntroUrl", target = "videoIntroUrl"),
            @Mapping(source = "latitude", target = "latitude"),
            @Mapping(source = "longitude", target = "longitude"),
            @Mapping(source = "preferredDistance", target = "preferredDistance"),
            @Mapping(source = "isEmailVerified", target = "isEmailVerified"),
            @Mapping(source = "isPhoneVerified", target = "isPhoneVerified"),
            @Mapping(source = "isProfileVerified", target = "isProfileVerified"),
            @Mapping(source = "createdAt", target = "createdAt"),
            @Mapping(source = "updatedAt", target = "updatedAt")
    })
    UserResponseDTO toUserResponseDTO(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mappings({
            @Mapping(source = "username", target = "username"),
            @Mapping(source = "email", target = "email"),
            @Mapping(source = "firstName", target = "firstName"),
            @Mapping(source = "lastName", target = "lastName"),
            @Mapping(source = "phoneNumber", target = "phoneNumber"),
            @Mapping(source = "playerLevel", target = "playerLevel"),
            @Mapping(source = "dateOfBirth", target = "dateOfBirth"),
            @Mapping(source = "genderIdentity", target = "genderIdentity"),
            @Mapping(source = "bio", target = "bio"),
            @Mapping(source = "videoIntroUrl", target = "videoIntroUrl"),
            @Mapping(source = "latitude", target = "latitude"),
            @Mapping(source = "longitude", target = "longitude"),
            @Mapping(source = "preferredDistance", target = "preferredDistance")
    })
    void updateUserFromDto(UserRequestDTO dto, @MappingTarget User entity);
}
