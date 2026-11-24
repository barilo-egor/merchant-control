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
import tgb.cryptoexchange.merchantcontrol.enums.FiatCurrency;
import tgb.cryptoexchange.merchantcontrol.repository.DealRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(DealService.class)
class DealServiceIntegrationTest {

    private static final FiatCurrency RUB = FiatCurrency.RUB;

    private static final FiatCurrency KZT = FiatCurrency.KZT;

    @Autowired
    private DealService dealService;

    @Autowired
    private DealRepository dealRepository;

    private static Deal deal(Merchant merchant, String appId, Long dealId, FiatCurrency fiatCurrency, Integer amount,
                             Instant createDate) {
        return Deal.builder()
                .merchant(merchant)
                .appId(appId)
                .dealId(dealId)
                .fiatCurrency(fiatCurrency)
                .amount(amount)
                .createDate(createDate)
                .build();
    }

    @BeforeEach
    void setUp() {
        dealRepository.deleteAll();
        dealRepository.save(deal(Merchant.ALFA_TEAM, "BULBA", 1L, RUB, 1234, Instant.now()));
        dealRepository.save(deal(Merchant.SETTLE_X, "TG", 2L, RUB, 1388, Instant.now()));
        dealRepository.save(deal(Merchant.ALFA_TEAM, "TG", 3L, RUB, 456, Instant.now()));
    }

    @Test
    @DisplayName("findByMerchant должен корректно суммировать сделки для мерчанта")
    void findByMerchant_ShouldReturnCorrectSummary() {
        DealSummaryDTO result = dealService.findByMerchant(Merchant.ALFA_TEAM, null);

        assertThat(result).isNotNull();
        assertThat(result.getTotalAmounts()).containsOnly(entry(RUB, 1690L));

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
                .extracting(DealSummaryDTO::getTotalAmounts, as(InstanceOfAssertFactories.MAP))
                .isEmpty();
        assertThat(result)
                .extracting(DealSummaryDTO::getDealIdsByAppId, as(InstanceOfAssertFactories.MAP))
                .isEmpty();
    }

    @Test
    @DisplayName("findByMerchant должен суммировать сделки раздельно по валютам, а рубль выводить первым")
    void findByMerchant_ShouldSumSeparatelyByCurrency() {
        Instant kztDealDate = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
        dealRepository.save(deal(Merchant.ALFA_TEAM, "TG", 4L, KZT, 1_250_000, kztDealDate));

        DealSummaryDTO result = dealService.findByMerchant(Merchant.ALFA_TEAM, null);

        assertThat(result.getTotalAmounts()).containsExactly(entry(RUB, 1690L), entry(KZT, 1_250_000L));
        assertThat(result.getDealIdsByAppId().get("TG")).containsExactly(3L, 4L);
        assertThat(result.getDealLastDate()).isEqualTo(kztDealDate);
    }

    @Test
    @DisplayName("findByMerchant должен брать только сделки не позднее указанной даты")
    void findByMerchant_ShouldCountOnlyDealsNotAfterDate() {
        Instant olderDealDate = Instant.parse("2026-01-01T12:00:00Z");
        dealRepository.save(deal(Merchant.BIT_ZONE, "TG", 10L, RUB, 100, olderDealDate));
        dealRepository.save(deal(Merchant.BIT_ZONE, "TG", 11L, KZT, 200, Instant.parse("2026-02-01T12:00:00Z")));

        DealSummaryDTO result = dealService.findByMerchant(Merchant.BIT_ZONE, Instant.parse("2026-01-15T12:00:00Z"));

        assertThat(result.getTotalAmounts()).containsOnly(entry(RUB, 100L));
        assertThat(result.getDealIdsByAppId()).containsOnly(entry("TG", List.of(10L)));
        assertThat(result.getDealLastDate()).isEqualTo(olderDealDate);
    }

    @Test
    @DisplayName("findByMerchants должен возвращать всех запрошенных мерчантов, а у мерчантов без положительных сумм — пустой totalAmounts")
    void findByMerchants_ShouldReturnAllRequestedMerchantsWithEmptyTotalsWhenNoPositiveDeals() {
        dealRepository.save(deal(Merchant.EXTASY_PAY, "TG", 15L, KZT, 0, Instant.now()));

        List<DealSummaryDTO> results = dealService.findByMerchants(
                List.of(Merchant.BIT_ZONE, Merchant.EXTASY_PAY, Merchant.SETTLE_X, Merchant.ALFA_TEAM));

        assertThat(results)
                .extracting(DealSummaryDTO::getMerchant)
                .containsExactly(Merchant.BIT_ZONE, Merchant.EXTASY_PAY, Merchant.SETTLE_X, Merchant.ALFA_TEAM);
        assertThat(results.get(0).getTotalAmounts()).isEmpty();
        assertThat(results.get(0).getDealIdsByAppId()).isEmpty();
        assertThat(results.get(1).getTotalAmounts()).isEmpty();
        assertThat(results.get(2).getTotalAmounts()).containsOnly(entry(RUB, 1388L));
        assertThat(results.get(2).getDealIdsByAppId()).containsOnly(entry("TG", List.of(2L)));
        assertThat(results.get(3).getTotalAmounts()).containsOnly(entry(RUB, 1690L));
    }

    @Test
    @DisplayName("findByMerchant не должен включать валюту с нулевой суммой в totalAmounts")
    void findByMerchant_ShouldExcludeZeroAmountCurrency() {
        dealRepository.save(deal(Merchant.ALFA_TEAM, "TG", 20L, KZT, 0, Instant.now()));

        DealSummaryDTO result = dealService.findByMerchant(Merchant.ALFA_TEAM, null);

        assertThat(result.getTotalAmounts()).containsOnly(entry(RUB, 1690L));
    }

    @Test
    @DisplayName("existsByMerchantAndCreateDateLessThanEqual должен находить старые сделки")
    void existsByMerchantAndCreateDateLessThanEqual_ShouldFindOlderDeals() {
        Merchant merchant = Merchant.ALFA_TEAM;
        Instant referenceDate = Instant.now().minusSeconds(60);

        dealRepository.save(deal(merchant, "BULBA", 10L, RUB, 5, referenceDate.minusSeconds(60)));
        boolean exists = dealRepository.existsByMerchantAndCreateDateLessThanEqual(merchant, referenceDate);

        assertThat(exists).isTrue();
    }

}
