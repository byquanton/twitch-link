package eu.byquanton.plugins.twitch_link.twitch;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.byquanton.plugins.twitch_link.twitch.response.helix.FollowedChannelsResponse;
import eu.byquanton.plugins.twitch_link.twitch.response.helix.HelixException;
import eu.byquanton.plugins.twitch_link.twitch.response.helix.StreamsResponse;
import eu.byquanton.plugins.twitch_link.twitch.response.helix.UserSubscriptionResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TwitchRequestUtil {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TwitchIntegration twitchIntegration;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    protected TwitchRequestUtil(TwitchIntegration twitchIntegration) {
        this.twitchIntegration = twitchIntegration;

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public boolean isUserFollowing(TwitchUser twitchUser, String broadcasterID) throws HelixException, IOException, InterruptedException {
        FollowedChannelsResponse response = checkUserFollow(twitchUser, broadcasterID);

        return !response.data().isEmpty();
    }

    public boolean isUserSubscribed(TwitchUser twitchUser, String broadcasterID) throws HelixException, IOException, InterruptedException {
        UserSubscriptionResponse response = getUserSubscription(twitchUser, broadcasterID);

        return !response.data().isEmpty();
    }

    public boolean isUserLive(TwitchUser twitchUser) throws HelixException, IOException, InterruptedException {
        StreamsResponse response = getStreams(twitchUser);

        return !response.data().isEmpty();
    }


    protected FollowedChannelsResponse checkUserFollow(TwitchUser twitchUser, String broadcasterId) throws IOException, InterruptedException, HelixException {
        String uri = twitchIntegration.getHelixEndpoint() + "channels/followed?broadcaster_id=" + broadcasterId + "&user_id=" + twitchUser.twitchUserId();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Authorization", "Bearer " + twitchUser.accessToken())
                .header("Client-Id", twitchIntegration.getClientID())
                .GET()
                .build();

        return getResponse(request, FollowedChannelsResponse.class);
    }


    protected UserSubscriptionResponse getUserSubscription(TwitchUser twitchUser, String broadcasterId) throws IOException, InterruptedException, HelixException {
        String uri = twitchIntegration.getHelixEndpoint() + "subscriptions/user?broadcaster_id=" + broadcasterId + "&user_id=" + twitchUser.twitchUserId();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Authorization", "Bearer " + twitchUser.accessToken())
                .header("Client-Id", twitchIntegration.getClientID())
                .GET()
                .build();

        return getResponse(request, UserSubscriptionResponse.class);
    }


    protected StreamsResponse getStreams(TwitchUser twitchUser) throws IOException, InterruptedException, HelixException {
        String uri = twitchIntegration.getHelixEndpoint() + "streams?user_id=" + twitchUser.twitchUserId();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Authorization", "Bearer " + twitchUser.accessToken())
                .header("Client-Id", twitchIntegration.getClientID())
                .GET()
                .build();

        return getResponse(request, StreamsResponse.class);
    }


    private <T> T getResponse(HttpRequest request, Class<T> responseType) throws IOException, InterruptedException, HelixException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), responseType);
        } else {
            throw HelixException.from(response);
        }
    }

}
