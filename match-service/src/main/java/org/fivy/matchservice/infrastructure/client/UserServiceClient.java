package org.fivy.matchservice.infrastructure.client;

import org.fivy.matchservice.api.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserServiceClient {
    @GetMapping("/api/v1/users/blocks/ids")
    ApiResponse<List<UUID>> getBlockedUserIdsWithWrapper(@RequestHeader("Authorization") String token);

    @GetMapping("/api/v1/users/blocks/check/{userId}")
    ApiResponse<Boolean> isUserBlockedWithWrapper(@RequestHeader("Authorization") String token,
                                                  @PathVariable("userId") UUID userId);
}