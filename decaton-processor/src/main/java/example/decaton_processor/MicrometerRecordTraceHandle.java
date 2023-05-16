package example.decaton_processor;


import com.linecorp.decaton.processor.DecatonProcessor;
import com.linecorp.decaton.processor.tracing.TracingProvider;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

final class MicrometerRecordTraceHandle extends MicrometerTraceHandle implements TracingProvider.RecordTraceHandle {
    MicrometerRecordTraceHandle(Tracer tracer, Span span) {
        super(tracer, span);
    }

    @Override
    public MicrometerProcessorTraceHandle childFor(DecatonProcessor<?> processor) {
        final var childSpan = tracer.nextSpan(span).name(processor.name()).start();
        return new MicrometerProcessorTraceHandle(tracer, childSpan);
    }
}
