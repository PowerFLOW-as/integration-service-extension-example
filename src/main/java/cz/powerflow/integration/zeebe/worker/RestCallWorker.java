package cz.powerflow.integration.zeebe.worker;

import cz.notix.zeebe.job.ZeebeWorker;
import cz.powerflow.integration.configuration.IntegrationServiceExtensionExampleConfiguration;
import cz.powerflow.integration.zeebe.helper.ZeebeVariablesFacade;
import cz.powerflow.integration.zeebe.variables.RestCallInput;
import cz.powerflow.integration.zeebe.variables.RestCallOutput;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@AllArgsConstructor
public class RestCallWorker extends ZeebeWorker {

    private static final Logger LOG = LoggerFactory.getLogger(RestCallWorker.class);
    private static final String REST_CALL_WORKER_NAME = "integration-service-extension-example-rest-call-worker";

    private static final String REST_CALL_SECURITY_HEADER_X_API_KEY = "x-api-key";

    private ZeebeVariablesFacade zeebeVariablesFacade;

    private IntegrationServiceExtensionExampleConfiguration integrationConfiguration;

    /**
     * @return The required identification of the worker. It must be the same in the Zeebe process XML definition.
     */
    @Override
    public String getType() {
        return REST_CALL_WORKER_NAME;
    }

    /**
     * Implementation of the worker.
     * It works with the input data that are mapped from the process payload in the process XLM definition.
     */
    @Override
    protected void work(JobClient client, ActivatedJob job) throws Exception {
        // Load inputWorkerData variables
        RestCallInput inputWorkerData = zeebeVariablesFacade.getInputVariables(job, RestCallInput.class);

        LOG.info("inputWorkerData: {}", inputWorkerData);
        LOG.info("Business correlation key given in the main starting HTTP API: {}",
                 inputWorkerData.getBusinessCorrelationKey());

        // The worker's specific implementation.
        URI uri = prepareURI();
        HttpEntity<Object> httpEntity = prepareHttpEntityAndHeaders(inputWorkerData);
        RestTemplate restTemplate = getRestTemplate(integrationConfiguration.getRestCallTimeoutInMs());

        // Rest call help variables
        Instant start = Instant.now();
        RestCallOutput output;

        try {
            ResponseEntity<?> response = restTemplate.exchange(uri, HttpMethod.GET, httpEntity, Object.class);

            Instant finish = Instant.now();
            long timeElapsedMs = Duration.between(start, finish).toMillis();
            LOG.info("Request finished in {} ms.", timeElapsedMs);

            LOG.trace("Response from API call is: {}", response);

            LOG.trace("Response is JSON: {}", response.getBody());
            output = RestCallOutput.builder()
                .responseCode(response.getStatusCode().value())
                .responseHeaders(convertHeadersToResponseMap(response.getHeaders()))
                .responseBody(response.getBody())
                .build();
        } catch (ResourceAccessException ex) {
            Instant finish = Instant.now();

            Map<String, String> responseBody;

            if (Objects.nonNull(ex.getCause().getMessage()) &&
                (ex.getCause().getMessage().indexOf("connect timed out") > 0 ||
                 Objects.equals(ex.getCause().getMessage(), "Read timed out"))) {

                LOG.warn("Connection timeout in : {}ms.", Duration.between(start, finish).toMillis());

                String message = "Request timed out after " + Duration.between(start, finish).toSeconds() + " seconds.";
                responseBody = prepareErrorResponseForResponseBodyZeebeVariable(message);

                output = RestCallOutput.builder()
                    .responseCode(HttpStatus.REQUEST_TIMEOUT.value())
                    .responseHeaders(new ArrayList<>())
                    .responseBody(responseBody)
                    .build();

            } else {
                final String warnMessage =
                    String.format("Rest call end with error. URL: %s is in network. It took : %sms.", uri,
                                  Duration.between(start, finish).toMillis());
                LOG.warn(warnMessage, ex);

                String message =
                    "URL not found(SERVICE_UNAVAILABLE). It took " + Duration.between(start, finish).toSeconds() +
                    " seconds.";
                responseBody = prepareErrorResponseForResponseBodyZeebeVariable(message);

                output = RestCallOutput.builder()
                    .responseCode(HttpStatus.SERVICE_UNAVAILABLE.value())
                    .responseHeaders(new ArrayList<>())
                    .responseBody(responseBody)
                    .build();
            }

        } catch (HttpClientErrorException ex) {
            Instant finish = Instant.now();

            final String warnMessage = String.format("Client returned error status (not 200 OK) It took: %sms.",
                                                     Duration.between(start, finish).toMillis());
            LOG.warn(warnMessage, ex);

            Map<String, String> responseBody = prepareErrorResponseForResponseBodyZeebeVariable(ex.getMessage());
            output = RestCallOutput.builder()
                .responseCode(ex.getStatusCode().value())
                .responseHeaders(convertHeadersToResponseMap(ex.getResponseHeaders()))
                .responseBody(responseBody)
                .build();
        }

        // Finishing worker by sending the complete command and by sending output variables into the Zeebe process.
        LOG.info("RestCallOutput variables from RestCallWorker are : {}", output);
        zeebeVariablesFacade.completeCommandWithOutputVariables(client, job, output);
    }

    private HttpEntity<Object> prepareHttpEntityAndHeaders(RestCallInput input) {
        // prepare headers
        HttpHeaders httpHeaders = prepareHeadersForRestCall();
        HttpEntity<Object> httpEntity = new HttpEntity<>(httpHeaders);
        return httpEntity;
    }

    private URI prepareURI() throws URISyntaxException {

        StringBuilder url = new StringBuilder(integrationConfiguration.getRestCallURL());

        LOG.info("Final URI address is: " + url);

        return new URI(url.toString());
    }

    private static Map<String, String> prepareErrorResponseForResponseBodyZeebeVariable(Object message) {
        Map<String, String> responseMap = new HashMap<>();

        responseMap.put("message", Objects.toString(message));
        return responseMap;
    }

    private List<Map<String, String>> convertHeadersToResponseMap(MultiValueMap<String, String> headers) {

        List<Map<String, String>> outputHeaders = new ArrayList<>();

        if (CollectionUtils.isEmpty(headers)) {
            return outputHeaders;
        }

        headers.forEach((name, values) -> {
            for (String value : values) {
                Map<String, String> outputHeader = new HashMap<>();
                outputHeader.put("name", name);
                outputHeader.put("value", value);
                outputHeaders.add(outputHeader);
            }
        });

        return outputHeaders;
    }

    private HttpHeaders prepareHeadersForRestCall() {
        LOG.trace("prepareHeadersForRestCall()");

        final HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        // Fill headers form security configuration
        httpHeaders.set(REST_CALL_SECURITY_HEADER_X_API_KEY, integrationConfiguration.getRestCallxApiKey());

        return httpHeaders;
    }

    private RestTemplate getRestTemplate(int timeoutInMilliseconds) {
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(timeoutInMilliseconds);
        clientHttpRequestFactory.setReadTimeout(timeoutInMilliseconds);
        return new RestTemplate(clientHttpRequestFactory);
    }


}
