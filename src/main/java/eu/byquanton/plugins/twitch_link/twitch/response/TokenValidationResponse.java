package eu.byquanton.plugins.twitch_link.twitch.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TokenValidationResponse(
        @JsonProperty("client_id") String clientId,
        @JsonProperty("login") String login,
        @JsonProperty("scopes") List<String> scopes,
        @JsonProperty("user_id") String userId,
        @JsonProperty("expires_in") int expiresIn
) {
}
