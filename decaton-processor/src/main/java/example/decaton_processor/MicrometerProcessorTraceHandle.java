package example.decaton_processor;


import com.linecorp.decaton.processor.tracing.TracingProvider.ProcessorTraceHandle;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

final class MicrometerProcessorTraceHandle extends MicrometerTraceHandle implements ProcessorTraceHandle {
    private Tracer.SpanInScope scope;

    MicrometerProcessorTraceHandle(Tracer tracer, Span span) {
        super(tracer, span);
    }

    @Override
    public void processingStart() {
        scope = tracer.withSpan(span);
    }

    @Override
    public void processingReturn() {
        span.event("return");
        scope.close();
    }
}
