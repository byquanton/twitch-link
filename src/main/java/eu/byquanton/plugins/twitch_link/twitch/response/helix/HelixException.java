package eu.byquanton.plugins.twitch_link.twitch.response.helix;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpResponse;

public class HelixException extends Exception {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final HelixExceptionResponse response;

    public HelixException(HelixExceptionResponse response) {
        super(response.message());
        this.response = response;
    }

    public HelixExceptionResponse getResponse() {
        return response;
    }

    public static HelixException from(HttpResponse<String> response) throws JsonProcessingException {
        return new HelixException(objectMapper.readValue(response.body(), HelixExceptionResponse.class));
    }
}
