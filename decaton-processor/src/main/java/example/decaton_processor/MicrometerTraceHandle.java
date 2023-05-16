
package example.decaton_processor;

import com.linecorp.decaton.processor.tracing.TracingProvider.TraceHandle;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

class MicrometerTraceHandle implements TraceHandle {
    protected final Tracer tracer;
    protected final Span span;

    MicrometerTraceHandle(Tracer tracer, Span span) {
        this.tracer = tracer;
        this.span = span;
    }

    @Override
    public void processingCompletion() {
        span.end();
    }
}
