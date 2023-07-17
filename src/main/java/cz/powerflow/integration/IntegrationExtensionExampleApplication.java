package cz.powerflow.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "cz.powerflow")
public class IntegrationExtensionExampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(IntegrationExtensionExampleApplication.class, args);
	}

}
