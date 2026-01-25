package tgb.cryptoexchange.merchantcontrol.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import tgb.cryptoexchange.commons.enums.Merchant;
import tgb.cryptoexchange.merchantcontrol.dto.DealSummaryDTO;
import tgb.cryptoexchange.merchantcontrol.entity.Deal;
import tgb.cryptoexchange.merchantcontrol.repository.DealRepository;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(DealService.class)
class DealServiceIntegrationTest {

    @Autowired
    private DealService dealService;

    @Autowired
    private DealRepository dealRepository;

    @BeforeEach
    void setUp() {
        dealRepository.deleteAll();
        dealRepository.save(
                Deal.builder().dealId(1L).appId("BULBA").merchant(Merchant.NEURAL_PAY).amount(1234).createDate(Instant.now())
                        .build());
        dealRepository.save(
                Deal.builder().dealId(2L).appId("TG").merchant(Merchant.SETTLE_X).amount(1388).createDate(Instant.now())
                        .build());
        dealRepository.save(
                Deal.builder().dealId(3L).appId("TG").merchant(Merchant.NEURAL_PAY).amount(456).createDate(Instant.now())
                        .build());
    }

    @Test
    @DisplayName("findByMerchant должен корректно суммировать сделки для мерчанта")
    void findByMerchant_ShouldReturnCorrectSummary() {
        DealSummaryDTO result = dealService.findByMerchant(Merchant.NEURAL_PAY);

        assertThat(result).isNotNull();
        assertThat(result.getTotalAmount()).isEqualTo(1690);
        assertThat(result.getDealIds()).hasSize(2);
        assertThat(result.getDealIds()).containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    @DisplayName("findByMerchant должен возвращать пустой DTO для мерчанта без сделок")
    void findByMerchant_ShouldReturnEmptySummary_WhenMerchantHasNoDeals() {
        DealSummaryDTO result = dealService.findByMerchant(Merchant.BIT_ZONE);

        assertThat(result).isNotNull();
        assertThat(result.getTotalAmount()).isZero();
        assertThat(result.getDealIds()).isEmpty();
    }

    @Test
    @DisplayName("existsByMerchantAndCreateDateLessThanEqual должен находить старые сделки")
    void existsByMerchantAndCreateDateLessThanEqual_ShouldFindOlderDeals() {
        Merchant merchant = Merchant.NEURAL_PAY;
        Instant referenceDate = Instant.now().minusSeconds(60);

        dealRepository.save(Deal.builder()
                .dealId(10L)
                .amount(5)
                .appId("BULBA")
                .merchant(merchant)
                .createDate(referenceDate.minusSeconds(60))
                .build());
        boolean exists = dealRepository.existsByMerchantAndCreateDateLessThanEqual(merchant, referenceDate);

        assertThat(exists).isTrue();
    }


}