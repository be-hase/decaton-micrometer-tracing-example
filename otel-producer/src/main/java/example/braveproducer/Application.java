package example.braveproducer;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.instrumentation.kafkaclients.v2_6.KafkaTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Properties;

@SpringBootApplication
@EnableScheduling
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public OpenTelemetry openTelemetry() {
        return OpenTelemetrySdk
                .builder()
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("kafka");
    }

    @Bean
    public Producer<String, String> producer(OpenTelemetry openTelemetry) {
        final var producerConfig = new Properties();
        producerConfig.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, System.getenv("KAFKA_BOOTSTRAP_SERVERS"));

        final var producer = new KafkaProducer<>(producerConfig, Serdes.String().serializer(), Serdes.String().serializer());
        return KafkaTelemetry.create(openTelemetry).wrap(producer);
    }

    @Component
    public static class CronBraveProducer {
        private static final Logger log = LoggerFactory.getLogger(CronBraveProducer.class);
        private final Producer<String, String> producer;
        private final Tracer tracer;

        CronBraveProducer(Producer<String, String> producer, Tracer tracer) {
            this.producer = producer;
            this.tracer = tracer;
        }

        @Scheduled(fixedRate = 5000)
        public void runProduce() {
            final var span = tracer.spanBuilder("cron").startSpan();
            try (final var scope = span.makeCurrent()) {
                producer.send(
                        new ProducerRecord<>(
                                "decaton-micrometer-tracing-example",
                                "from-otel-producer. traceId=" + span.getSpanContext().getTraceId()
                        ),
                        (r, e) -> {
                            if (e == null) {
                                log.info("Sent to kafka. metadata.offset={}", r.offset());
                            } else {
                                log.error("Failed to send to kafka", e);
                            }
                        }
                );
            } finally {
                span.end();
            }
        }
    }
}
