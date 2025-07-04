package eu.byquanton.plugins.twitch_link.twitch.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TwitchAPIExceptionResponse(
        @JsonProperty("status") int status,
        @JsonProperty("message") String message
) {

}
