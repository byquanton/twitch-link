package eu.byquanton.plugins.twitch_link.twitch.response.helix;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HelixExceptionResponse(
        @JsonProperty("error") String error,
        @JsonProperty("status") int status,
        @JsonProperty("message") String message
) {

}
