package example.decaton_processor;

import brave.internal.Nullable;
import com.linecorp.decaton.processor.tracing.TracingProvider;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MicrometerTracingProvider implements TracingProvider {
    private final Tracer tracer;
    private final Propagator propagator;

    public MicrometerTracingProvider(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    @Override
    public MicrometerRecordTraceHandle traceFor(ConsumerRecord<?, ?> record, String subscriptionId) {
        return new MicrometerRecordTraceHandle(
                tracer,
                propagator.extract(record.headers(), GETTER).name("decaton").tag("subscriptionId", subscriptionId).start()
        );
    }

    private static final Propagator.Getter<Headers> GETTER = new Propagator.Getter<Headers>() {
        @Override
        public String get(Headers carrier, String key) {
            return lastStringHeader(carrier, key);
        }

        @Nullable
        static String lastStringHeader(Headers headers, String key) {
            Header header = headers.lastHeader(key);
            if (header == null || header.value() == null) return null;
            return new String(header.value(), UTF_8);
        }
    };
}
