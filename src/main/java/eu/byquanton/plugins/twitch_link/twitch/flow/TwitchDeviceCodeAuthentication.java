package eu.byquanton.plugins.twitch_link.twitch.flow;

import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.byquanton.plugins.twitch_link.twitch.TwitchIntegration;
import eu.byquanton.plugins.twitch_link.twitch.response.DeviceAuthorizationResponse;
import eu.byquanton.plugins.twitch_link.twitch.response.TokenResponse;
import eu.byquanton.plugins.twitch_link.twitch.response.TwitchAPIException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TwitchDeviceCodeAuthentication {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String clientID;
    private final String scopes;
    private final URI deviceURL;
    private final URI tokenURL;

    public TwitchDeviceCodeAuthentication(TwitchIntegration twitchIntegration) {
        this.clientID = twitchIntegration.getClientID();
        this.scopes = twitchIntegration.getScopes();
        this.deviceURL = URI.create(twitchIntegration.getOauthEndpoint() + "device");
        this.tokenURL = URI.create(twitchIntegration.getOauthEndpoint() + "token");
    }


    public DeviceAuthorizationResponse startDeviceAuthorizationFlow() throws TwitchAPIException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(this.deviceURL)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("client_id=" + clientID + "&scopes=" + scopes))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        try {
            return objectMapper.readValue(response.body(), DeviceAuthorizationResponse.class);
        } catch (DatabindException e) {
            throw TwitchAPIException.from(response);
        }
    }

    public TokenResponse obtainToken(String deviceCode) throws TwitchAPIException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(tokenURL)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("client_id=" + clientID + "&scope=" + scopes + "&device_code=" + deviceCode + "&grant_type=urn:ietf:params:oauth:grant-type:device_code"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        try {
            return objectMapper.readValue(response.body(), TokenResponse.class);
        } catch (DatabindException e) {
            throw TwitchAPIException.from(response);
        }
    }
}
