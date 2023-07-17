package cz.powerflow.integration.zeebe.variables;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class RestCallOutput {

    @JsonProperty(value = "responseCode", required = true)
    private Integer responseCode;

    @JsonProperty(value = "responseHeaders", required = true)
    private List<Map<String, String>> responseHeaders;

    @JsonProperty(value = "responseBody", required = true)
    private Object responseBody;
}
