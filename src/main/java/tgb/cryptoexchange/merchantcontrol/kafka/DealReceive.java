package tgb.cryptoexchange.merchantcontrol.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import org.apache.kafka.common.serialization.Deserializer;
import tgb.cryptoexchange.commons.enums.Merchant;
import tgb.cryptoexchange.merchantcontrol.exception.DeserializeEventException;

import java.nio.charset.StandardCharsets;

@Data
public class DealReceive {

    private Long dealId;

    private String appId;

    private Merchant merchant;

    private Integer amount;

    public static class KafkaDeserializer implements Deserializer<DealReceive> {

        private final ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());

        @Override
        public DealReceive deserialize(String topic, byte[] data) {
            try {
                if (data == null) return null;
                return objectMapper.readValue(data, DealReceive.class);
            } catch (Exception e) {
                throw new DeserializeEventException("Error occurred while deserializer value: " + new String(data, StandardCharsets.UTF_8), e);
            }
        }
    }

}
