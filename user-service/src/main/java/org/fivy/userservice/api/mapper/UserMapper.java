package org.fivy.userservice.api.mapper;

import org.fivy.userservice.api.dto.UserRequestDTO;
import org.fivy.userservice.api.dto.UserResponseDTO;
import org.fivy.userservice.api.dto.update.ProfileResponse;
import org.fivy.userservice.api.dto.update.UpdateProfileRequest;
import org.fivy.userservice.domain.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {


    UserResponseDTO toUserResponseDTO(User user);

    @Mapping(target = "playerLevel", source = "playerLevel")
    User toUser(UserRequestDTO userRequestDTO);

    void updateUserFromDto(UpdateProfileRequest request, @MappingTarget User user);

    ProfileResponse toProfileResponse(User user);

}
