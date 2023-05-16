package example.decaton_processor;

import brave.kafka.clients.KafkaTracing;
import com.linecorp.decaton.processor.DecatonProcessor;
import com.linecorp.decaton.processor.ProcessingContext;
import com.linecorp.decaton.processor.TaskMetadata;
import com.linecorp.decaton.processor.runtime.*;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import org.apache.kafka.clients.CommonClientConfigs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public brave.Tracing braveTracing() {
        return brave.Tracing.newBuilder()
                // default is B3Propagation
                //.propagationFactory(new W3CPropagation())
                .build();
    }

    @Bean
    public KafkaTracing braveKafkaTracing(brave.Tracing tracing) {
        return KafkaTracing.create(tracing);
    }

    @Bean
    public OpenTelemetry openTelemetry() {
        return OpenTelemetrySdk
                .builder()
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();
    }

    @Bean
    public io.opentelemetry.api.trace.Tracer openTelemetryTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracerProvider().get("io.micrometer.micrometer-tracing");
    }

    // Using Brave
//    @Bean
//    public Tracer micrometerTracer(brave.Tracing tracing) {
//        return new BraveTracer(tracing.tracer(), new BraveCurrentTraceContext(tracing.currentTraceContext()), new BraveBaggageManager());
//    }

    // Using OpenTelemetry
    @Bean
    public Tracer micrometerTracer(io.opentelemetry.api.trace.Tracer tracer) {
        final var otelCurrentTraceContext = new OtelCurrentTraceContext();
        return new OtelTracer(
                tracer,
                otelCurrentTraceContext,
                event -> {
                }
        );
    }

    @Bean
    public ProcessorSubscription decatonProcessorSubscription(
            brave.Tracing braveTracing,
            KafkaTracing braveKafkaTracing,
            io.opentelemetry.api.trace.Tracer openTelemetryTracer,
            Tracer micrometerTracer
    ) {
        final var processorsBuilder = ProcessorsBuilder
                .consuming("decaton-micrometer-tracing-example", new StringTaskExtractor())
                .thenProcess(new SampleProcessor());

        final var consumerConfig = new Properties();
        consumerConfig.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, System.getenv("KAFKA_BOOTSTRAP_SERVERS"));
        consumerConfig.put(CommonClientConfigs.GROUP_ID_CONFIG, System.getenv("USER") + "-decaton-micrometer-tracing-example");

        return SubscriptionBuilder
                .newBuilder("decaton-micrometer-tracing-example")
                .consumerConfig(consumerConfig)
                .enableTracing(new MicrometerTracingProvider(micrometerTracer, new OtelPropagator(ContextPropagators.create(W3CTraceContextPropagator.getInstance()), openTelemetryTracer)))
                // .enableTracing(new MicrometerTracingProvider(micrometerTracer, new BravePropagator(braveTracing))
                //.enableTracing(new BraveTracingProvider(kafkaTracing))
                .processorsBuilder(processorsBuilder)
                .buildAndStart();
    }

    static class SampleProcessor implements DecatonProcessor<String> {
        private static final Logger log = LoggerFactory.getLogger(SampleProcessor.class);

        @Override
        public void process(ProcessingContext<String> context, String task) {
            log.info("[{}] Processing task. task={}", currentTraceId(), task);
        }

        private String currentTraceId() {
            final var braveContext = brave.Tracing.current().currentTraceContext().get();
            if (braveContext != null) {
                return braveContext.traceIdString();
            }
            return Span.current().getSpanContext().getTraceId();
        }
    }

    static class StringTaskExtractor implements TaskExtractor<String> {
        @Override
        public DecatonTask<String> extract(byte[] bytes) {
            final var taskMetadata = TaskMetadata.builder()
                    .timestampMillis(System.currentTimeMillis())
                    .sourceApplicationId("sample")
                    .build();
            return new DecatonTask<>(taskMetadata, new String(bytes, StandardCharsets.UTF_8), bytes);
        }
    }
}
