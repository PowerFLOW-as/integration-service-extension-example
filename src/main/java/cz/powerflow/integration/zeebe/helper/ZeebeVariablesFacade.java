package cz.powerflow.integration.zeebe.helper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import cz.powerflow.integration.exception.ValidationException;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ZeebeVariablesFacade {

    private static final Logger LOG = LoggerFactory.getLogger(ZeebeVariablesFacade.class);

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    public JsonObject getPayload(ActivatedJob activatedJob) {
        return getVariableObject(activatedJob, "payload");
    }

    public JsonObject getVariableObject(ActivatedJob activatedJob, String name) {
        String variablesAsJsonString = activatedJob.getVariables();

        LOG.debug("variables content: {}", variablesAsJsonString);

        JsonObject variables = GSON.fromJson(variablesAsJsonString, JsonObject.class);
        if (variables != null && variables.has(name) && variables.get(name).isJsonObject()) {
            LOG.debug("return variables with name {} for jobKey {}", name, activatedJob.getKey());
            return variables.getAsJsonObject(name);
        } else {
            final String msg = "Cannot find " + name + " in activated job data";
            LOG.error(msg + ": " + variablesAsJsonString);
            throw new ValidationException(msg, ": " + variablesAsJsonString);
        }
    }

    /**
     * Get input object as given type. Exception thrown at unmarshalling error.
     *
     * @param activatedJob
     * @param inputType
     * @param <T>
     * @return
     */
    public <T> T getInputVariables(ActivatedJob activatedJob, Class<T> inputType) {
        JsonObject input = getVariableObject(activatedJob, "input");
        return GSON.fromJson(input, inputType);
    }

    public String getOutputVariables(ActivatedJob activatedJob, Object output) {
        JsonObject variables = new JsonObject();

        JsonObject payload = getPayload(activatedJob);
        JsonElement outputObject = GSON.toJsonTree(output);

        // This conversion would be used if the variable should be de-serialized
        // variables.add("headers", getVariableObject(activatedJob, "headers"));
        variables.add("payload", payload);
        variables.add("output", outputObject);

        return GSON.toJson(variables);
    }

    /**
     * Complete command and override output in payload with given object.
     *
     * @param jobClient
     * @param activatedJob
     * @param output
     */
    public void completeCommandWithOutputVariables(JobClient jobClient, ActivatedJob activatedJob, Object output) {
        jobClient.newCompleteCommand(activatedJob.getKey())
            .variables(getOutputVariables(activatedJob, output))
            .send()
            .join();
    }
}
