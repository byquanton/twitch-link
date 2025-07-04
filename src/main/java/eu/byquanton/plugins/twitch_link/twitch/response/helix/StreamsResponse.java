package eu.byquanton.plugins.twitch_link.twitch.response.helix;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record StreamsResponse(
        @JsonProperty("data") List<StreamData> data,
        @JsonProperty("pagination") Pagination pagination
) {
    public record StreamData(
            @JsonProperty("id") String id,
            @JsonProperty("user_id") String userId,
            @JsonProperty("user_login") String userLogin,
            @JsonProperty("user_name") String userName,
            @JsonProperty("game_id") String gameId,
            @JsonProperty("game_name") String gameName,
            @JsonProperty("type") String type,
            @JsonProperty("title") String title,
            @JsonProperty("tags") List<String> tags,
            @JsonProperty("viewer_count") int viewerCount,
            @JsonProperty("started_at") String startedAt,
            @JsonProperty("language") String language,
            @JsonProperty("thumbnail_url") String thumbnailUrl,
            @JsonProperty("tag_ids") List<String> tagIds,
            @JsonProperty("is_mature") boolean isMature
    ) {
    }

    public record Pagination(
            @JsonProperty("cursor") String cursor
    ) {
    }
}
