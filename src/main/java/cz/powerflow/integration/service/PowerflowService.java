package cz.powerflow.integration.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cz.notix.logging.tracing.MDCTracingInfo;
import cz.powerflow.integration.configuration.PowerflowConfiguration;
import cz.powerflow.integration.dto.pwfservice.PwfUserVerifyApi;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static cz.notix.logging.tracing.HttpRequestTracingInfo.OPEN_TRACE_TRACE_ID;

/**
 * In case of needed REST (HTTP) API, token verification is required.
 * The verification process is done by calling PWF Service's API that verifies a user and the token's validity.
 */
@Service
@AllArgsConstructor
public class PowerflowService {

    private static final Logger LOG = LoggerFactory.getLogger(PowerflowService.class);
    private static final Gson GSON = new GsonBuilder().create();

    private static final RestTemplate restTemplate = new RestTemplate();
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String X_REQUEST_ID = "X-Request-Id";

    private static final String VERIFY_URL = "/api/client/rest/token/verify";

    private final PowerflowConfiguration powerflowConfiguration;

    /**
     * @param token Bearer token to be verified by PWF Service
     * @return PwfUserVerifyApi object
     */
    public PwfUserVerifyApi verifyToken(String token) {
        LOG.info("verifyToken(token={}", "X");
        LOG.trace("verifyToken(token={}", token);

        try {

            URI uri = new URI(powerflowConfiguration.getPowerflowServiceBaseUrl() + VERIFY_URL);

            LOG.info("verifyToken PWF service(url={})", uri);

            // prepare request
            HttpEntity request = getHttpEntityRequestWithoutBody(token);

            LOG.trace("Headers of request: {}", request.getHeaders());

            // send REST request
            ResponseEntity<Object> response = restTemplate.exchange(uri, HttpMethod.GET, request, Object.class);

            if (!HttpStatus.OK.equals(response.getStatusCode())) {
                LOG.error(
                    "VerifyToken PWF service failed(method=GET, uri={}, responseStatusCode={}, responseBody={}, responseHeaders={})",
                    uri, response.getStatusCode(), response.getBody(), response.getHeaders());
            }

            LOG.trace("response={}", response);

            PwfUserVerifyApi pwfUser =
                GSON.fromJson(GSON.toJson(response.getBody()), PwfUserVerifyApi.class);

            LOG.debug("Deserialized pwfUser={}", pwfUser);
            return pwfUser;

        } catch (Exception e) {
            throw new RuntimeException("Powerflow service rest call failed.", e);
        }
    }

    private HttpEntity getHttpEntityRequestWithoutBody(String authorization) {
        // prepare request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (authorization.startsWith(BEARER_PREFIX)) {
            headers.set("authorization", authorization);
        } else {
            LOG.trace("Authorization do not have Bearer prefix. Adding it to the value. authorization={}", authorization);
            headers.set("authorization", BEARER_PREFIX + authorization);
        }

        // Trace IDs for tracing purpose in logs, etc.
        // It's highly recommended to implement these across entire back-end applications
        headers.set(OPEN_TRACE_TRACE_ID, MDC.get(MDCTracingInfo.REQUEST_ID_LOGGING_PARAM));
        headers.set(X_REQUEST_ID, MDC.get(MDCTracingInfo.REQUEST_ID_LOGGING_PARAM));

        return new HttpEntity(headers);
    }
}
