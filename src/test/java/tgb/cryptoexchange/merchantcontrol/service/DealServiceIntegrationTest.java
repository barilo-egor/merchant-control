package tgb.cryptoexchange.merchantcontrol.service;

import org.assertj.core.api.InstanceOfAssertFactories;
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
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.as;
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
                Deal.builder().dealId(1L).appId("BULBA").merchant(Merchant.ALFA_TEAM).amount(1234).createDate(Instant.now())
                        .build());
        dealRepository.save(
                Deal.builder().dealId(2L).appId("TG").merchant(Merchant.SETTLE_X).amount(1388).createDate(Instant.now())
                        .build());
        dealRepository.save(
                Deal.builder().dealId(3L).appId("TG").merchant(Merchant.ALFA_TEAM).amount(456).createDate(Instant.now())
                        .build());
    }

    @Test
    @DisplayName("findByMerchant должен корректно суммировать сделки для мерчанта")
    void findByMerchant_ShouldReturnCorrectSummary() {
        DealSummaryDTO result = dealService.findByMerchant(Merchant.ALFA_TEAM, null);

        assertThat(result).isNotNull();
        assertThat(result.getTotalAmount()).isEqualTo(1690);

        List<Long> allDealIds = result.getDealIdsByAppId().values().stream()
                .flatMap(Collection::stream)
                .toList();

        assertThat(allDealIds)
                .hasSize(2)
                .containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    @DisplayName("findByMerchant должен возвращать пустой DTO для мерчанта без сделок")
    void findByMerchant_ShouldReturnEmptySummary_WhenMerchantHasNoDeals() {
        DealSummaryDTO result = dealService.findByMerchant(Merchant.BIT_ZONE, null);

        assertThat(result)
                .isNotNull()
                .returns(0, DealSummaryDTO::getTotalAmount)
                .extracting(DealSummaryDTO::getDealIdsByAppId, as(InstanceOfAssertFactories.MAP))
                .isEmpty();
    }

    @Test
    @DisplayName("existsByMerchantAndCreateDateLessThanEqual должен находить старые сделки")
    void existsByMerchantAndCreateDateLessThanEqual_ShouldFindOlderDeals() {
        Merchant merchant = Merchant.ALFA_TEAM;
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