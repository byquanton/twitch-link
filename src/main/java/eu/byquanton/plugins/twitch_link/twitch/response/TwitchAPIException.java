package eu.byquanton.plugins.twitch_link.twitch.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpResponse;

public class TwitchAPIException extends Exception {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final TwitchAPIExceptionResponse response;

    public TwitchAPIException(TwitchAPIExceptionResponse response) {
        super(response.message());
        this.response = response;
    }

    public TwitchAPIExceptionResponse getResponse() {
        return response;
    }

    public static TwitchAPIException from(HttpResponse<String> response) throws JsonProcessingException {
        return new TwitchAPIException(objectMapper.readValue(response.body(), TwitchAPIExceptionResponse.class));
    }
}
