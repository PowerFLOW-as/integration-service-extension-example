package cz.powerflow.integration.zeebe.variables;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Class describing input (mapped) data of the Rest Call worker that comes from the Zeebe process's payload.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestCallInput {

    @JsonProperty(value = "businessCorrelationKey", required = true)
    private String businessCorrelationKey;

}
