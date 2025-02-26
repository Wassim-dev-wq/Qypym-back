package org.fivy.matchservice.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.api.dto.response.UserPushTokenResponse;
import org.fivy.matchservice.api.dto.request.UserPushTokenRequest;
import org.fivy.matchservice.application.service.UserPushTokenService;
import org.fivy.matchservice.domain.entity.UserPushToken;
import org.fivy.matchservice.domain.repository.UserPushTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPushTokenServiceImpl implements UserPushTokenService {

    private final UserPushTokenRepository userPushTokenRepository;

    @Override
    @Transactional
    public UserPushTokenResponse registerToken(UserPushTokenRequest request) {
        UserPushToken existingToken = userPushTokenRepository.findByExpoToken(request.getExpoToken());
        if (existingToken == null) {
            UserPushToken newToken = UserPushToken.builder()
                    .userId(request.getUserId())
                    .expoToken(request.getExpoToken())
                    .build();
            UserPushToken saved = userPushTokenRepository.save(newToken);
            log.info("Registered new push token for user {}: {}", request.getUserId(), request.getExpoToken());
            return mapToDto(saved);
        } else {
            log.info("Push token already registered for user {}: {}", request.getUserId(), request.getExpoToken());
            return mapToDto(existingToken);
        }
    }

    @Override
    @Transactional
    public void deleteToken(UUID userId, String expoToken) {
        userPushTokenRepository.deleteByUserIdAndExpoToken(userId, expoToken);
        log.info("Deleted push token for user {}: {}", userId, expoToken);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPushTokenResponse> getTokensByUserId(UUID userId) {
        List<UserPushToken> tokens = userPushTokenRepository.findByUserId(userId);
        return tokens.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private UserPushTokenResponse mapToDto(UserPushToken token) {
        return UserPushTokenResponse.builder()
                .id(token.getId())
                .userId(token.getUserId())
                .expoToken(token.getExpoToken())
                .createdAt(token.getCreatedAt())
                .build();
    }
}
