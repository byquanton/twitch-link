package eu.byquanton.plugins.twitch_link.twitch.response.helix;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record UserSubscriptionResponse(
        @JsonProperty("data") List<SubscriptionData> data
) {
    public record SubscriptionData(
            @JsonProperty("broadcaster_id") String broadcasterId,
            @JsonProperty("broadcaster_login") String broadcasterLogin,
            @JsonProperty("broadcaster_name") String broadcasterName,
            @JsonProperty("gifter_id") String gifterId,
            @JsonProperty("gifter_login") String gifterLogin,
            @JsonProperty("gifter_name") String gifterName,
            @JsonProperty("is_gift") boolean isGift,
            @JsonProperty("tier") String tier
    ) {
    }
}
