package cz.powerflow.integration.configuration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * The application's main configuration.
 */
@Data
@Configuration
public class IntegrationServiceExtensionExampleConfiguration {

    /// The REST worker's configuration

    @Value("${integration.service.extension.example.rest.security.x-api-key}")
    private String restCallxApiKey;

    @Value("${integration.service.extension.example.rest.url}")
    private String restCallURL;

    @Value("${integration.service.extension.example.rest.call.timeout.ms}")
    private Integer restCallTimeoutInMs;

}
