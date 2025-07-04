package eu.byquanton.plugins.twitch_link.twitch;

import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.byquanton.plugins.twitch_link.twitch.response.TokenRefreshResponse;
import eu.byquanton.plugins.twitch_link.twitch.response.TokenValidationResponse;
import eu.byquanton.plugins.twitch_link.twitch.response.TwitchAPIException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TwitchTokenValidation {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final TwitchIntegration twitchIntegration;
    private final URI validateUrl;
    private final URI tokenUrl;

    protected TwitchTokenValidation(TwitchIntegration twitchIntegration) {
        this.twitchIntegration = twitchIntegration;
        this.validateUrl = URI.create(twitchIntegration.getOauthEndpoint() + "validate");
        this.tokenUrl = URI.create(twitchIntegration.getOauthEndpoint() + "token");
    }


    protected TokenValidationResponse validate(String access_token) throws TwitchAPIException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(validateUrl)
                .header("Authorization", "Bearer " + access_token)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        try {
            return objectMapper.readValue(response.body(), TokenValidationResponse.class);
        } catch (DatabindException e) {
            throw TwitchAPIException.from(response);
        }
    }

    protected TokenRefreshResponse refresh(String refresh_token) throws TwitchAPIException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(tokenUrl)

                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "grant_type=refresh_token&refresh_token=" + refresh_token + "&client_id=" + twitchIntegration.getClientID()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        try {
            return objectMapper.readValue(response.body(), TokenRefreshResponse.class);
        } catch (DatabindException e) {
            throw TwitchAPIException.from(response);
        }
    }


}
