package cz.powerflow.integration.configuration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Optional configuration in need of token verification.
 */
@Data
@Configuration
public class PowerflowConfiguration {

    @Value("${powerflow.service.url:http://service:8080/service}")
    private String powerflowServiceBaseUrl;

}
