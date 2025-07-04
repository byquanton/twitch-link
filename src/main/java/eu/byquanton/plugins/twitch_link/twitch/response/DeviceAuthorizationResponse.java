package eu.byquanton.plugins.twitch_link.twitch.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DeviceAuthorizationResponse(@JsonProperty("device_code") String deviceCode,
                                          @JsonProperty("expires_in") String expiresIn,
                                          @JsonProperty("interval") String interval,
                                          @JsonProperty("user_code") String userCode,
                                          @JsonProperty("verification_uri") String verificationUri) {
}