# decaton-micrometer-tracing-example

## Run Producer Application

### Using Brave

```shell
KAFKA_BOOTSTRAP_SERVERS="..." ./gradlew :brave-producer:bootRun
```

### Using OpenTelemetry

```shell
KAFKA_BOOTSTRAP_SERVERS="..." ./gradlew :otel-producer:bootRun
```

## Run Decaton Processor Application

### Using Brave Bridge

```shell
KAFKA_BOOTSTRAP_SERVERS="..." ./gradlew :decaton-processor:bootRun
```

### Using OpenTelemetry Bridge

```shell
KAFKA_BOOTSTRAP_SERVERS="..." OTEL_MODE="true" ./gradlew :decaton-processor:bootRun
```
