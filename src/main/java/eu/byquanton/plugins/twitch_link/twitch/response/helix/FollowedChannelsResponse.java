package eu.byquanton.plugins.twitch_link.twitch.response.helix;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record FollowedChannelsResponse(
        @JsonProperty("total") int total,
        @JsonProperty("data") List<FollowedChannel> data,
        @JsonProperty("pagination") Pagination pagination
) {
    public record FollowedChannel(
            @JsonProperty("broadcaster_id") String broadcasterId,
            @JsonProperty("broadcaster_login") String broadcasterLogin,
            @JsonProperty("broadcaster_name") String broadcasterName,
            @JsonProperty("followed_at") String followedAt
    ) {
    }

    public record Pagination(
            @JsonProperty("cursor") String cursor
    ) {
    }
}
