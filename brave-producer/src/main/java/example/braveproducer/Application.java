package example.braveproducer;

import brave.Tracing;
import brave.kafka.clients.KafkaTracing;
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
    public Tracing tracing() {
        return Tracing.newBuilder().build();
    }

    @Bean
    public KafkaTracing kafkaTracing(Tracing tracing) {
        return KafkaTracing.create(tracing);
    }

    @Bean
    public Producer<String, String> producer(KafkaTracing kafkaTracing) {
        final var producerConfig = new Properties();
        producerConfig.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, System.getenv("KAFKA_BOOTSTRAP_SERVERS"));


        final var producer = new KafkaProducer<>(producerConfig, Serdes.String().serializer(), Serdes.String().serializer());
        return kafkaTracing.producer(producer);
    }

    @Component
    public static class CronBraveProducer {
        private static final Logger log = LoggerFactory.getLogger(CronBraveProducer.class);
        private final Producer<String, String> producer;
        private final Tracing tracing;

        CronBraveProducer(Producer<String, String> producer, Tracing tracing) {
            this.producer = producer;
            this.tracing = tracing;
        }

        @Scheduled(fixedRate = 5000)
        public void runProduce() {
            final var span = tracing.tracer().nextSpan().name("cron").start();
            try (final var scope = tracing.tracer().withSpanInScope(span)) {
                producer.send(
                        new ProducerRecord<>(
                                "decaton-micrometer-tracing-example",
                                "from-brave-producer. traceId=" + span.context().traceIdString()
                        ),
                        (r, e) -> {
                            if (e == null) {
                                log.info("Sent to kafka. metadata.offset={}", r.offset());
                            } else {
                                log.error("Failed to send to kafka", e);
                            }
                        }
                );
            } catch (Exception e) {
                span.error(e);
                throw e;
            } finally {
                span.finish();
            }
        }
    }
}
