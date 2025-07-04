package eu.byquanton.plugins.twitch_link.twitch.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TokenRefreshResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("scope") List<String> scopes,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") int expiresIn
) {
}
