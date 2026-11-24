package tgb.cryptoexchange.merchantcontrol.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tgb.cryptoexchange.commons.enums.Merchant;
import tgb.cryptoexchange.exception.BadRequestException;
import tgb.cryptoexchange.merchantcontrol.dto.DealSummaryDTO;
import tgb.cryptoexchange.merchantcontrol.entity.Deal;
import tgb.cryptoexchange.merchantcontrol.enums.FiatCurrency;
import tgb.cryptoexchange.merchantcontrol.kafka.DealReceive;
import tgb.cryptoexchange.merchantcontrol.projection.MerchantDealIdProjection;
import tgb.cryptoexchange.merchantcontrol.projection.MerchantDealTotalProjection;
import tgb.cryptoexchange.merchantcontrol.repository.DealRepository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DealServiceTest {

    private static final FiatCurrency RUB = FiatCurrency.RUB;

    private static final FiatCurrency KZT = FiatCurrency.KZT;

    @Mock
    private DealRepository dealRepository;

    @InjectMocks
    private DealService dealService;

    private static MerchantDealTotalProjection total(Merchant merchant, FiatCurrency fiatCurrency, long totalAmount,
                                                     Instant dealLastDate) {
        MerchantDealTotalProjection projection = mock(MerchantDealTotalProjection.class);
        when(projection.getMerchant()).thenReturn(merchant);
        when(projection.getFiatCurrency()).thenReturn(fiatCurrency);
        when(projection.getTotalAmount()).thenReturn(totalAmount);
        when(projection.getDealLastDate()).thenReturn(dealLastDate);
        return projection;
    }

    private static MerchantDealIdProjection dealId(Merchant merchant, String appId, Long dealId) {
        MerchantDealIdProjection projection = mock(MerchantDealIdProjection.class);
        when(projection.getMerchant()).thenReturn(merchant);
        when(projection.getAppId()).thenReturn(appId);
        when(projection.getDealId()).thenReturn(dealId);
        return projection;
    }

    @Test
    @DisplayName("save должен сохранять сделку с исходной суммой и её фиатной валютой")
    void save_ShouldMapAndSave() {
        Instant createDate = Instant.now();
        DealReceive request = new DealReceive();
        request.setDealId(123L);
        request.setAppId("BULBA");
        request.setMerchant(Merchant.ALFA_TEAM);
        request.setFiatCurrency(KZT);
        request.setAmount(322);
        request.setCreateDate(createDate);

        dealService.save(request);

        ArgumentCaptor<Deal> dealCaptor = ArgumentCaptor.forClass(Deal.class);
        verify(dealRepository, times(1)).save(dealCaptor.capture());
        assertThat(dealCaptor.getValue())
                .returns(123L, Deal::getDealId)
                .returns("BULBA", Deal::getAppId)
                .returns(Merchant.ALFA_TEAM, Deal::getMerchant)
                .returns(KZT, Deal::getFiatCurrency)
                .returns(322, Deal::getAmount)
                .returns(createDate, Deal::getCreateDate);
    }

    @Test
    @DisplayName("Если фиатная валюта не пришла, save должен сохранить сделку в рублях.")
    void save_ShouldSaveAsRub_WhenFiatCurrencyIsMissing() {
        DealReceive request = new DealReceive();
        request.setDealId(123L);
        request.setFiatCurrency(null);
        request.setAmount(500);

        dealService.save(request);

        ArgumentCaptor<Deal> dealCaptor = ArgumentCaptor.forClass(Deal.class);
        verify(dealRepository).save(dealCaptor.capture());
        assertThat(dealCaptor.getValue())
                .returns(123L, Deal::getDealId)
                .returns(RUB, Deal::getFiatCurrency)
                .returns(500, Deal::getAmount);
    }

    @Test
    @DisplayName("findByMerchant должен возвращать суммы сделок мерчанта раздельно по валютам")
    void findByMerchant_ShouldReturnDealSummary_WhenFound() {
        Merchant merchant = Merchant.ALFA_TEAM;
        MerchantDealTotalProjection rubTotal = total(merchant, RUB, 1256, Instant.parse("2026-01-01T12:00:00Z"));
        MerchantDealTotalProjection kztTotal = total(merchant, KZT, 5000, Instant.parse("2026-02-01T12:00:00Z"));
        MerchantDealIdProjection firstAppFirstDealId = dealId(merchant, "app1", 1L);
        MerchantDealIdProjection firstAppSecondDealId = dealId(merchant, "app1", 2L);
        MerchantDealIdProjection secondAppDealId = dealId(merchant, "app2", 3L);
        when(dealRepository.findTotalsByMerchantIn(List.of(merchant))).thenReturn(List.of(rubTotal, kztTotal));
        when(dealRepository.findAllByMerchantInOrderByAppIdAscDealIdAsc(List.of(merchant))).thenReturn(List.of(
                firstAppFirstDealId, firstAppSecondDealId, secondAppDealId));

        DealSummaryDTO result = dealService.findByMerchant(merchant, null);

        assertThat(result.getMerchant()).isEqualTo(merchant);
        assertThat(result.getTotalAmounts()).containsExactly(entry(RUB, 1256L), entry(KZT, 5000L));
        assertThat(result.getDealIdsByAppId()).containsOnly(
                entry("app1", List.of(1L, 2L)),
                entry("app2", List.of(3L)));
        assertThat(result.getDealLastDate()).isEqualTo(Instant.parse("2026-02-01T12:00:00Z"));
    }

    @Test
    @DisplayName("findByMerchant должен запрашивать из базы только сделки не позднее указанной даты")
    void findByMerchant_ShouldQueryDealsNotAfterDate() {
        Merchant merchant = Merchant.ALFA_TEAM;
        Instant dealLastDate = Instant.parse("2026-03-01T12:00:00Z");
        MerchantDealTotalProjection rubTotal = total(merchant, RUB, 500, Instant.parse("2026-01-01T12:00:00Z"));
        MerchantDealIdProjection firstDealId = dealId(merchant, "app1", 1L);
        when(dealRepository.findTotalsByMerchantInAndCreateDateLessThanEqual(List.of(merchant), dealLastDate))
                .thenReturn(List.of(rubTotal));
        when(dealRepository.findAllByMerchantInAndCreateDateLessThanEqualOrderByAppIdAscDealIdAsc(List.of(merchant), dealLastDate))
                .thenReturn(List.of(firstDealId));

        DealSummaryDTO result = dealService.findByMerchant(merchant, dealLastDate);

        assertThat(result.getTotalAmounts()).containsOnly(entry(RUB, 500L));
        assertThat(result.getDealIdsByAppId()).containsOnly(entry("app1", List.of(1L)));
    }

    @Test
    @DisplayName("deleteByMerchant  должен вызывать репозиторий, если сделки существуют")
    void deleteByMerchant_ShouldExecute_WhenExists() {
        Merchant merchant = Merchant.ALFA_TEAM;
        Instant now = Instant.now();

        when(dealRepository.existsByMerchantAndCreateDateLessThanEqual(eq(merchant), any(Instant.class)))
                .thenReturn(true);

        dealService.deleteByMerchantAndDate(merchant, now);

        verify(dealRepository).deleteByMerchantAndCreateDateLessThanEqual(eq(merchant), eq(now));
    }

    @Test
    @DisplayName("deleteByMerchant должен пробросить исключение BadRequestException, если тикета нет")
    void deleteByMerchant_ShouldNotExecute_WhenNotExists() {
        Merchant merchant = Merchant.ALFA_TEAM;
        when(dealRepository.existsByMerchantAndCreateDateLessThanEqual(eq(merchant), any(Instant.class)))
                .thenReturn(false);

        assertThatExceptionOfType(BadRequestException.class)
                .isThrownBy(() -> dealService.deleteByMerchantAndDate(merchant, Instant.now()));

        verify(dealRepository, never()).deleteByMerchantAndCreateDateLessThanEqual(any(), any());
    }

    @Test
    @DisplayName("findByMerchant должен возвращать пустой DTO, если сделок не найдено")
    void findByMerchant_ShouldReturnEmptySummary_WhenNoDealsFound() {
        Merchant merchant = Merchant.ALFA_TEAM;
        when(dealRepository.findTotalsByMerchantIn(List.of(merchant))).thenReturn(Collections.emptyList());
        when(dealRepository.findAllByMerchantInOrderByAppIdAscDealIdAsc(List.of(merchant))).thenReturn(Collections.emptyList());

        DealSummaryDTO result = dealService.findByMerchant(merchant, null);

        assertThat(result)
                .isNotNull()
                .returns(merchant, DealSummaryDTO::getMerchant);
        assertThat(result.getTotalAmounts()).isEmpty();
        assertThat(result.getDealIdsByAppId()).isEmpty();

        verify(dealRepository).findTotalsByMerchantIn(List.of(merchant));
    }

    @Test
    @DisplayName("findByMerchant не должен включать в totalAmounts валюты с нулевой суммой")
    void findByMerchant_ShouldExcludeZeroTotalAmounts() {
        Merchant merchant = Merchant.ALFA_TEAM;
        MerchantDealTotalProjection rubTotal = total(merchant, RUB, 1256, Instant.parse("2026-01-01T12:00:00Z"));
        MerchantDealTotalProjection zeroKztTotal = total(merchant, KZT, 0, Instant.parse("2026-02-01T12:00:00Z"));
        MerchantDealIdProjection firstDealId = dealId(merchant, "app1", 1L);
        when(dealRepository.findTotalsByMerchantIn(List.of(merchant))).thenReturn(List.of(rubTotal, zeroKztTotal));
        when(dealRepository.findAllByMerchantInOrderByAppIdAscDealIdAsc(List.of(merchant))).thenReturn(List.of(firstDealId));

        DealSummaryDTO result = dealService.findByMerchant(merchant, null);

        assertThat(result.getTotalAmounts()).containsOnly(entry(RUB, 1256L));
        assertThat(result.getDealLastDate()).isEqualTo(Instant.parse("2026-02-01T12:00:00Z"));
    }

    @Test
    @DisplayName("findByMerchants должен возвращать всех запрошенных мерчантов, включая мерчантов без сделок с пустым totalAmounts")
    void findByMerchants_ShouldIncludeMerchantsWithoutDeals() {
        List<Merchant> merchants = List.of(Merchant.ALFA_TEAM, Merchant.BIT_ZONE);
        MerchantDealTotalProjection rubTotal = total(Merchant.ALFA_TEAM, RUB, 1256, Instant.parse("2026-01-01T12:00:00Z"));
        MerchantDealIdProjection firstDealId = dealId(Merchant.ALFA_TEAM, "app1", 1L);
        when(dealRepository.findTotalsByMerchantIn(merchants)).thenReturn(List.of(rubTotal));
        when(dealRepository.findAllByMerchantInOrderByAppIdAscDealIdAsc(merchants)).thenReturn(List.of(firstDealId));

        List<DealSummaryDTO> results = dealService.findByMerchants(merchants);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getMerchant()).isEqualTo(Merchant.ALFA_TEAM);
        assertThat(results.get(0).getTotalAmounts()).containsOnly(entry(RUB, 1256L));
        assertThat(results.get(0).getDealIdsByAppId()).containsOnly(entry("app1", List.of(1L)));
        assertThat(results.get(1).getMerchant()).isEqualTo(Merchant.BIT_ZONE);
        assertThat(results.get(1).getTotalAmounts()).isEmpty();
        assertThat(results.get(1).getDealIdsByAppId()).isEmpty();
        assertThat(results.get(1).getDealLastDate()).isNull();
    }

    @Test
    @DisplayName("findByMerchants должен возвращать пустой список без обращения к БД при пустом списке мерчантов")
    void findByMerchants_ShouldReturnEmptyList_WhenMerchantsEmpty() {
        List<DealSummaryDTO> results = dealService.findByMerchants(Collections.emptyList());

        assertThat(results).isEmpty();
        verifyNoInteractions(dealRepository);
    }

}
