package example.decaton_processor;

import brave.Tracing;
import brave.kafka.clients.KafkaTracing;
import com.linecorp.decaton.processor.DecatonProcessor;
import com.linecorp.decaton.processor.ProcessingContext;
import com.linecorp.decaton.processor.TaskMetadata;
import com.linecorp.decaton.processor.runtime.*;
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
    public Tracing tracing() {
        return Tracing.newBuilder().build();
    }

    @Bean
    public KafkaTracing kafkaTracing(Tracing tracing) {
        return KafkaTracing.create(tracing);
    }

    @Bean
    public ProcessorSubscription decatonProcessorSubscription(Tracing tracing, KafkaTracing kafkaTracing) {
        final var processorsBuilder = ProcessorsBuilder
                .consuming("decaton-micrometer-tracing-example", new StringTaskExtractor())
                .thenProcess(new SampleProcessor(tracing));

        final var consumerConfig = new Properties();
        consumerConfig.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, System.getenv("KAFKA_BOOTSTRAP_SERVERS"));
        consumerConfig.put(CommonClientConfigs.GROUP_ID_CONFIG, System.getenv("USER") + "-decaton-micrometer-tracing-example");

        return SubscriptionBuilder
                .newBuilder("decaton-micrometer-tracing-example")
                .consumerConfig(consumerConfig)
                .enableTracing(new BraveTracingProvider(kafkaTracing))
                .processorsBuilder(processorsBuilder)
                .buildAndStart();
    }

    static class SampleProcessor implements DecatonProcessor<String> {
        private static final Logger log = LoggerFactory.getLogger(SampleProcessor.class);
        private final Tracing tracing;

        SampleProcessor(Tracing tracing) {
            this.tracing = tracing;
        }


        @Override
        public void process(ProcessingContext<String> context, String task) {
            log.info("[{}] Processing task. task={}", tracing.currentTraceContext().get().traceIdString(), task);
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
