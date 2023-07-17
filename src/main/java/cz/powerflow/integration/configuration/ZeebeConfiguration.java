package cz.powerflow.integration.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.powerflow.integration.zeebe.worker.RestCallWorker;
import cz.notix.zeebe.component.ZeebeHealthChecker;
import cz.notix.zeebe.job.ZeebeInit;
import cz.notix.zeebe.job.ZeebeInitBuilder;
import cz.notix.zeebe.job.ZeebeWorker;
import cz.notix.zeebe.job.ZeebeWorkersBuilder;
import cz.notix.zeebe.service.ZeebeService;
import cz.notix.zeebe.service.impl.ZeebeServiceImpl;
import cz.notix.zeebe.util.ZeebePropertiesUtil;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.impl.ZeebeClientBuilderImpl;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Required configuration to connect to the Zeebe broker.
 * Part of the configuration is list of custom Zeebe processes to be deployed.
 * The registration of Zeebe workers is also a part of the configuration.
 */
@Data
@Configuration
public class ZeebeConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(ZeebeConfiguration.class);

    private static final String ZEEBE_PROCESSES_DIR = "zeebe-processes/";

    private final Environment env;

    /**
     * The main property to connect the application to the Zeebe broker.
     */
    @Value("${zeebe.client.broker.contact-point}")
    private String brokerContactPoint;

    /**
     * The duration (ms) between two connection status checks into the Zeebe broker.
     */
    @Value("${zeebe.init.broker.healthcheck-timeout:#{2000L}}")
    private long healthCheckTimeout;

    /**
     * Every Zeebe worker injection should be here.
     */
    @Autowired
    private RestCallWorker restCallWorker;

    /**
     * Initialization of this bean is required.
     */
    @Bean(name = "zeebeVariablesObjectMapper")
    public ObjectMapper zeebeVariablesObjectMapper() {
        return new ObjectMapper();
    }

    @Autowired
    public ZeebeConfiguration(Environment env) {
        this.env = env;
    }

    /**
     * Required Zeebe Client bean declaration is needed in order to be connected into the Zeebe broker.
     */
    @Bean
    public ZeebeClient zeebeClient() {
        Properties properties = ZeebePropertiesUtil.getProperties(env);
        properties.keySet().forEach(key -> LOG.debug("Zeebe configuration: {}={}", key, env.getProperty((String) key)));

        return new ZeebeClientBuilderImpl()
            .usePlaintext()
            .gatewayAddress(brokerContactPoint)
            .withProperties(properties)
            .build();
    }

    /**
     * Required bean with a list of defined Zeebe processes.
     * It's needed for the processes to be deployed.
     */
    @Bean
    public ClassPathResource[] processes() {

        ClassPathResource[] processes = new ClassPathResource[]{
            new ClassPathResource(ZEEBE_PROCESSES_DIR + "integration-extension-example-rest-call-process.bpmn")
        };

        Arrays.stream(processes)
            .forEach(p -> LOG.info("Zeebe configuration contains a bpmn file: {}", p.getFilename()));

        Arrays.stream(processes).filter(p -> !p.exists()).collect(Collectors.toList())
            .forEach(p -> LOG.error("{} is missing.", p.getFilename()));

        return processes;
    }

    /**
     * Required bean with a list of Zeebe workers.
     * It's needed for the workers in order to pull jobs from the Zeebe broker.
     */
    @Bean
    public List<ZeebeWorker> workers() {
        return new ZeebeWorkersBuilder(new ObjectMapper())
            .addWorker(restCallWorker)
            .build();
    }

    /**
     * Zeebe Service is used for a Zeebe process start or custom deploy, it's not used in this example.
     */
    @Bean
    public ZeebeService zeebeService() {
        return new ZeebeServiceImpl().useZeebeClient(zeebeClient());
    }


    /**
     * Required bean definition.
     */
    @Bean
    @Scope(scopeName = ConfigurableBeanFactory.SCOPE_SINGLETON)
    @ConditionalOnProperty(prefix = "templating.zeebe", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ZeebeInit zeebeInit() {

        return new ZeebeInitBuilder(zeebeClient())
            .setWorkers(workers())
            .setBpmnFiles(processes())
            .setHealthCheckTimeout(healthCheckTimeout)
            .build().start();
    }

    /**
     * Required bean definition.
     */
    @Bean(name = "zeebeHealthChecker")
    @Scope(scopeName = ConfigurableBeanFactory.SCOPE_SINGLETON)
    @ConditionalOnProperty(prefix = "templating.zeebe", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ZeebeHealthChecker zeebeHealthChecker() {
        return new ZeebeHealthChecker(zeebeClient());
    }

}

