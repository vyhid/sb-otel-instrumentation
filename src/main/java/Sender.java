import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import io.opentelemetry.extension.annotations.WithSpan;

import java.util.List;

public class Sender {

//    Env variables
//    OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:8200
//    VM args
//    -javaagent:"C:\Users/avyhidn/Downloads/opentelemetry-javaagent-9.jar"
//    -Dotel.resource.attributes=service.name=sb-sender-process

    public static void main(String[] args) {
        ServiceBusSenderClient sender = new ServiceBusClientBuilder()
                .connectionString(AsyncReceiver.newCs)
                .sender()
                .topicName("otel-test")
                .buildClient();

        // When you are done using the sender, dispose of it.
        sendMsg(sender);
        sender.close();
    }

    @WithSpan
    static void sendMsg(ServiceBusSenderClient sender){
        ServiceBusMessage msg1 = new ServiceBusMessage("Hello world").setMessageId("1");
        List<ServiceBusMessage> messages = List.of(msg1);
        sender.sendMessages(messages);
    }
}
