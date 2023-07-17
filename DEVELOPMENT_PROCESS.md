# Development and testing of PowerFLOW Integration Service's extension

## Prerequisites and recommendations

Before the implementation, there are few aspects to consider:

- Do we have already a running (deployed) PowerFLOW environment?
- Do we have enabled and running PowerFLOW Integration Service deployed on the environment?
- Do we have new configurations for the new integration process type already defined by an Integration Designer?
- Do we have new configurations ready to be tested by developers on the runtime (running/deployed) environment?
- Do we have access to the PowerFLOW libraries <code>cz.notix.zeebe-fw</code> and optionally <code>cz.notix.logging</code>?
  - Library <code>cz.notix.logging</code> is recommended when creating HTTP API. It serves for propagation of trace ID into MDC.

## Development workflow

Let's assume that we are going to implement a new integration process (defined by a Zeebe process) along with a Zeebe
worker.

One of the possible ways (recommended) to implement and test the new process and worker is to have a running PowerFLOW environment
(deployed on a server/k8s/...).
At least one of the pre-defined PowerFLOW integrations should be tested and executed successfully.
The environment must contain a definition of the new PowerFLOW Integration along with defined variable mappings.

Once we run this new application locally, it should be connected to the Zeebe broker running on the environment.
The application will share the broker with the deployed PowerFLOW Integration Service.
In the logs, we can see the new process is deployed onto the broker.
The process ends successfully with the message <code>All processes were successfully deployed.</code>.
The information about a successful connection of the worker is represented by the message <code>Starting worker, type: <worker_type></code>.
PowerFLOW's Zeebe-FW library handles connectivity into the Zeebe broker and when the application is successfully connected,
it logs the <code>Health Check, state: RUNNING</code> message.

The two recommended ways of testing the new integration while developing are:
- By creating a new process using the starting HTTP API.
  - The API is defined by the URL <code>https://[hostname]/integration-ms/v1/integration/start</code>.
  - HTTP headers must be defined according to the integration's security configuration.
  - The request body has the following template:
  - ```
    {
      "integrationCode": "<integration_code_according_to_its_configuration>",
      "integrationPayload": {
        "businessCorrelationKey": "<businessCorrelationKeyValue>"
      }
    }
    ```
    - The template serves also for this app's use case. 
    - It's possible to extend the template's request body. For this purpose the <code>integrationPayload</code> object must be extended.
- By creating a new process via Integration Task defined in the PowerFLOW process.


## Maven settings - PowerFLOW Libraries access

Let's ensure that the Maven settings.xml file contains the PowerFLOW mirror from the example:

``` The example from the Maven settings.xml file
<settings>
  <mirrors>
    <mirror>
      <id>powerflow-nexus</id>
      <mirrorOf>*</mirrorOf>
      <url>https://repo.powerflow.cloud/repository/maven-powerflow/</url>
    </mirror>
  </mirrors>
  <servers>
    <server>
      <id>powerflow-nexus</id>
      <username>Fill your username here</username>
      <password>Fill your password here</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>powerflow-nexus</id>
      <repositories>
        <repository>
          <id>central</id>
          <url>http://central</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>true</enabled></snapshots>
        </repository>
      </repositories>
     <pluginRepositories>
        <pluginRepository>
          <id>central</id>
          <url>http://central</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>true</enabled></snapshots>
        </pluginRepository>
      </pluginRepositories>
    </profile>
  </profiles>
  <activeProfiles>
    <!--make the profile active all the time -->
    <activeProfile>powerflow-nexus</activeProfile>
  </activeProfiles>
</settings>
```

## The application code

### Configuration
- <code>ZeebeConfiguration</code> includes:
  - connection properties to the Zeebe broker,
  - list of workers and processes and
  - other mandatory beans.
- <code>IntegrationServiceExtensionExampleConfiguration</code> includes the application-specific properties.
  - E.g. worker properties, process properties, any other business-specific properties
- <code>PowerflowConfiguration</code> includes URL to the PowerFLOW Services if an integration is needed.
  - E.g. in case of PowerFLOW token verification or any other use case.

### Worker implementation
The class RestCallWorker implements the worker's execution logic. It starts with loading of worker's input data.
It ends with sending the complete command into Zeebe broker along with worker's output data.

Worker's input data are represented by its own class (RestCallInput). 
The output data are represented by the class RestCallOutput.

Class ZeebeVariablesFacade helps with deserialization of Zeebe variables.

### Zeebe process
The files integration-extension-example-rest-call-process.bpmn represents an example of Zeebe process.
It contains one worker that is identified as <code>integration-service-extension-example-rest-call-worker</code>.
Worker's configuration contains input and output mappings. 
In the example, we can see a propagation of businessCorrelationKey from the process's payload into the worker.
We can see also a propagation of the workers result back into the process's payload as output variables.

### Zeebe process error propagation
We recommend handling errore in two main ways:
1. Explicit error handling.
   1. If there's no requirement to throw a Zeebe process incident, it's recommended to handle an exception explicitly.
   2. In this case, the information about the exception and the error is propagated into the Zeebe process payload.
   3. The worker is finishes successfully and the Zeebe process continues normally.
   4. The handling of an unexpected state should be implemented within the Zeebe process definition (using e.g. gateways).
   5. See the example of <code>ResourceAccessException</code> handling in <code>RestCallWorker</code>.
2. Implicit error handling.
   1. By default, if an unhandled exception is thrown within the worker's execution logic, the Zeebe process will end with
   an incident.
   2. The incident event is handled by the PowerFLOW zeebe-fw library, and information such as stack trace 
automatically persisted into the Zeebe process payload.. 
   3. The information is stored within the Zeebe process payload - specifically in:
      1. <code>payload.errorCode</code>,
      2. <code>payload.errorData</code>, 
      3. <code>payload.errorMessage</code> and 
      4. <code>payload.exception</code> paths.
   4. The incident can be seen in the Zeebe monitor's Incidents tab.

### Logging and profiles
The <code>application.properties</code> and <code>application-dev.properties</code> files are 
example of the application configuration.
They contain the required application-specific, integration-specific, and common configurations along with 
example values.
The file <code>logback-spring.xml</code> contains the recommended logging pattern used within 
entire PowerFLOW (Java) stack.

### Docker
Dockerfile contains standard commands used for building Spring Boot applications in PowerFLOW.

### Running Zeebe cluster locally
For an experimental usage, there's <code>docker_zeebe</code> folder containing Shell scripts for creating and 
removing necessary folders, along with <code>docker-compose.yml</code> and <code>.env</code> files.

The first step is to run the <code>create_zeebe_volumes_dir.sh</code> script. After the folders are created, one may 
start Docker services by <code>docker-compose up -d</code> command.

The only use case for this example is to verify that the application is connected to the Zeebe broker and that
the process deployment is working.