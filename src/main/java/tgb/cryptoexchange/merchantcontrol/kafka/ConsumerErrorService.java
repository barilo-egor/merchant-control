package tgb.cryptoexchange.merchantcontrol.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ConsumerErrorService {

    public void handle(ConsumerRecord<?, ?> consumerRecord, Exception e) {
        log.error("Ошибка обработки записи: key={}, value={} partition={} offset={} ",
                consumerRecord.key(), consumerRecord.value(), consumerRecord.partition(), consumerRecord.offset(), e);
    }

}
