package org.fivy.notificationservice.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportRequestDto {

    @NotNull(message = "User ID is required")
    @Schema(description = "The ID of the user submitting the request")
    private UUID userId;

    @NotBlank(message = "Email is required")
    @Schema(description = "The email of the user")
    private String email;

    @NotBlank(message = "First name is required")
    @Schema(description = "The first name of the user")
    private String firstName;

    @Schema(description = "The last name of the user")
    private String lastName;

    @Schema(description = "The username of the user")
    private String username;

    @NotBlank(message = "Category is required")
    @Schema(description = "The category of the support request")
    private String category;

    @NotBlank(message = "Subject is required")
    @Size(max = 100, message = "Subject must be less than 100 characters")
    @Schema(description = "The subject of the support request")
    private String subject;

    @NotBlank(message = "Message is required")
    @Size(max = 2000, message = "Message must be less than 2000 characters")
    @Schema(description = "The detailed message of the support request")
    private String message;
}