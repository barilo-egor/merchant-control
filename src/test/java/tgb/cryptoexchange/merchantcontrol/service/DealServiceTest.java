package tgb.cryptoexchange.merchantcontrol.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tgb.cryptoexchange.commons.enums.Merchant;
import tgb.cryptoexchange.exception.BadRequestException;
import tgb.cryptoexchange.merchantcontrol.dto.DealSummaryDTO;
import tgb.cryptoexchange.merchantcontrol.entity.Deal;
import tgb.cryptoexchange.merchantcontrol.kafka.DealReceive;
import tgb.cryptoexchange.merchantcontrol.repository.DealRepository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DealServiceTest {

    @Mock
    private DealRepository dealRepository;

    @InjectMocks
    private DealService dealService;

    @Test
    @DisplayName("save должен корректно мапить запрос и сохранять сделку")
    void save_ShouldMapAndSave() {
        DealReceive request = new DealReceive();
        request.setDealId(123L);
        request.setAppId("BULBA");
        request.setMerchant(Merchant.ALFA_TEAM);
        request.setAmount(322);

        dealService.save(request);

        verify(dealRepository, times(1)).save(any(Deal.class));
    }

    @Test
    @DisplayName("findByMerchant должен возвращать общую сумму сделок для мерчанта")
    void findById_ShouldReturnDealSummary_WhenFound() {
        Merchant merchant = Merchant.ALFA_TEAM;
        String appId = "app1";
        Deal deal1 = Deal.builder().dealId(1L).appId(appId).merchant(merchant).amount(500).build();
        Deal deal2 = Deal.builder().dealId(2L).appId(appId).merchant(merchant).amount(756).build();
        List<Deal> deals = List.of(deal1, deal2);

        when(dealRepository.findAllByMerchant(merchant)).thenReturn(deals);

        DealSummaryDTO result = dealService.findByMerchant(merchant, null);

        assertNotNull(result);
        assertEquals(1256, result.getTotalAmount());
        assertNotNull(result.getDealIdsByAppId());
        assertTrue(result.getDealIdsByAppId().containsKey(appId));
        assertEquals(2, result.getDealIdsByAppId().get(appId).size());
        assertTrue(result.getDealIdsByAppId().get(appId).containsAll(List.of(1L, 2L)));

        verify(dealRepository, times(1)).findAllByMerchant(merchant);
    }

    @Test
    @DisplayName("findByMerchant должен возвращать пустой DTO с нулевой суммой, если сделок не найдено")
    void findByMerchant_ShouldReturnEmptySummary_WhenNoDealsFound() {
        Merchant merchant = Merchant.ALFA_TEAM;
        when(dealRepository.findAllByMerchant(merchant)).thenReturn(Collections.emptyList());

        DealSummaryDTO result = dealService.findByMerchant(merchant, null);

        assertNotNull(result, "Результат не должен быть null");
        assertEquals(0, result.getTotalAmount(), "Сумма должна быть 0");
        assertNotNull(result.getDealIdsByAppId(), "Мапа ID по appId не должна быть null");
        assertTrue(result.getDealIdsByAppId().isEmpty(), "Мапа ID по appId должна быть пустой");

        verify(dealRepository).findAllByMerchant(merchant);
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

}