import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.extension.annotations.WithSpan;


public class AsyncReceiver {

    public static String newCs = "<your-connection-string>";

//    Env variables
//    OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:8200
//    VM args
//    -javaagent:"C:\Users/avyhidn/Downloads/opentelemetry-javaagent-9.jar"
//    -Dotel.resource.attributes=service.name=sb-receiver-process
    public static void main(String[] args) throws InterruptedException {
        ServiceBusReceiverAsyncClient asyncClient = new ServiceBusClientBuilder()
                .connectionString(newCs)
                .receiver()
                .topicName("otel-test")
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
        System.out.println("msg.body : " + msg.getBody());
        System.out.println("msg.properties : " + msg.getApplicationProperties());
        Span currentSpan = Span.current();
        doSomeJob();
        System.out.println("trace-id: " + currentSpan.getSpanContext().getTraceId());
    }
}



