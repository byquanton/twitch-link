package eu.byquanton.plugins.twitch_link.twitch.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") String expiresIn,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("scope") List<String> scope
) {
}
