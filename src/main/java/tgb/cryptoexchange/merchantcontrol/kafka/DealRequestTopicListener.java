package tgb.cryptoexchange.merchantcontrol.kafka;

import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Profile({"!kafka-disabled"})
public class DealRequestTopicListener {

    private final DealRequestProcessorService requestProcessorService;

    public DealRequestTopicListener(DealRequestProcessorService requestProcessorService) {
        this.requestProcessorService = requestProcessorService;
    }

    @KafkaListener(topics = "${kafka.topic.deal.request}", groupId = "${kafka.group-id}",
            containerFactory = "dealListenerFactory")
    public void receive(@Payload DealReceive dealRequest) {
        requestProcessorService.process(dealRequest);
    }

}
