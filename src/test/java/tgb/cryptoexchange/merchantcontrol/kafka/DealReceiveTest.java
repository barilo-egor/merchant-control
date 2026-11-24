package tgb.cryptoexchange.merchantcontrol.kafka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tgb.cryptoexchange.commons.enums.Merchant;
import tgb.cryptoexchange.merchantcontrol.enums.FiatCurrency;
import tgb.cryptoexchange.merchantcontrol.exception.DeserializeEventException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class DealReceiveTest {

    private final DealReceive.KafkaDeserializer deserializer = new DealReceive.KafkaDeserializer();

    @Test
    @DisplayName("deserialize должен читать из сообщения фиатную валюту и исходную сумму сделки")
    void deserialize_ShouldReadFiatCurrencyAndAmount() {
        String message = """
                {"dealId": 123, "appId": "BULBA", "merchant": "ALFA_TEAM", "fiatCurrency": "KZT",
                 "amount": 1250000, "createDate": "2026-01-01T12:00:00Z"}
                """;

        DealReceive dealReceive = deserializer.deserialize("deal-request", message.getBytes(StandardCharsets.UTF_8));

        assertThat(dealReceive.getDealId()).isEqualTo(123L);
        assertThat(dealReceive.getAppId()).isEqualTo("BULBA");
        assertThat(dealReceive.getMerchant()).isEqualTo(Merchant.ALFA_TEAM);
        assertThat(dealReceive.getFiatCurrency()).isEqualTo(FiatCurrency.KZT);
        assertThat(dealReceive.getAmount()).isEqualTo(1250000);
        assertThat(dealReceive.getCreateDate()).isEqualTo(Instant.parse("2026-01-01T12:00:00Z"));
    }

    @Test
    @DisplayName("deserialize должен оставлять фиатную валюту пустой, если её нет в сообщении")
    void deserialize_ShouldLeaveFiatCurrencyNull_WhenItIsAbsent() {
        String message = """
                {"dealId": 123, "appId": "BULBA", "merchant": "ALFA_TEAM", "amount": 100,
                 "createDate": "2026-01-01T12:00:00Z"}
                """;

        DealReceive dealReceive = deserializer.deserialize("deal-request", message.getBytes(StandardCharsets.UTF_8));

        assertThat(dealReceive.getFiatCurrency()).isNull();
    }

    @Test
    @DisplayName("deserialize должен пробрасывать DeserializeEventException при неизвестной фиатной валюте")
    void deserialize_ShouldThrow_WhenFiatCurrencyIsUnknown() {
        String message = """
                {"dealId": 123, "appId": "BULBA", "merchant": "ALFA_TEAM", "fiatCurrency": "USD",
                 "amount": 100, "createDate": "2026-01-01T12:00:00Z"}
                """;

        assertThatExceptionOfType(DeserializeEventException.class)
                .isThrownBy(() -> deserializer.deserialize("deal-request", message.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("deserialize должен пробрасывать DeserializeEventException при неверном сообщении")
    void deserialize_ShouldThrow_WhenMessageIsInvalid() {
        byte[] message = "not json".getBytes(StandardCharsets.UTF_8);

        assertThatExceptionOfType(DeserializeEventException.class)
                .isThrownBy(() -> deserializer.deserialize("deal-request", message));
    }

}
