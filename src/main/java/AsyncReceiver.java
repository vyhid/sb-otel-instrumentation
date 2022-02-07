import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.extension.annotations.WithSpan;

import java.util.List;
import java.util.Map;


public class AsyncReceiver {

    public static String newCs = "<connection-string>";
    private final static Tracer TRACER = GlobalOpenTelemetry.getTracer("app");

    private static final Iterable<String> KEYS = List.of(com.azure.core.util.tracing.Tracer.DIAGNOSTIC_ID_KEY);
    private static final TextMapGetter<ServiceBusReceivedMessage> SERVICE_BUS_CONTEXT_GETTER =
            new TextMapGetter<ServiceBusReceivedMessage>() {
                @Override
                public Iterable<String> keys(ServiceBusReceivedMessage carrier) {
                    return KEYS;
                }

                @Override
                public String get(ServiceBusReceivedMessage carrier, String key) {
                    if ("traceparent".equals(key)) {
                        Object value = carrier.getApplicationProperties().get(com.azure.core.util.tracing.Tracer.DIAGNOSTIC_ID_KEY);
                        return value == null ? null : value.toString();
                    }

                    return null;
                }
            };

//    Env variables
//    OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:8200
//    VM args
//    -javaagent:"C:\Users/avyhidn/Downloads/opentelemetry-javaagent-9.jar"
//    -Dotel.resource.attributes=service.name=sb-receiver-process
    public static void main(String[] args) throws InterruptedException {
        ServiceBusReceiverAsyncClient asyncClient = new ServiceBusClientBuilder()
                .connectionString(newCs)
                .receiver()
                .topicName("testme")
                .subscriptionName("otel-test-subscription")
                .buildAsyncClient();


        asyncClient.receiveMessages()
                .subscribe(AsyncReceiver::accept);
        Thread.sleep(50_00);

        asyncClient.close();

    }

    @WithSpan
    static void doSomeJob() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("job 1");
    }


    private static void accept(ServiceBusReceivedMessage msg) {
        Context remoteContext = W3CTraceContextPropagator.getInstance().extract(Context.current(), msg, SERVICE_BUS_CONTEXT_GETTER);

        Span span = TRACER.spanBuilder("ServiceBus.process").setParent(remoteContext).setSpanKind(SpanKind.CONSUMER).startSpan();
        try (Scope scope = Context.current().with(span).makeCurrent()) {
            System.out.println("msg.body : " + msg.getBody());
            System.out.println("msg.properties : " + msg.getApplicationProperties());
            doSomeJob();
            System.out.println("trace-id: " + span.getSpanContext().getTraceId());
            span.setStatus(StatusCode.OK);
        } catch (Throwable t) {
            span.recordException(t);
            span.setStatus(StatusCode.ERROR);
        } finally {
            span.end();
        }
    }
}



