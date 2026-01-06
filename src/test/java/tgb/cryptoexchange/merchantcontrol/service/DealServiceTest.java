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

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatException;
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
        request.setMerchant(Merchant.NEURAL_PAY);
        request.setAmount(322);

        dealService.save(request);

        verify(dealRepository, times(1)).save(any(Deal.class));
    }

    @Test
    @DisplayName("findByMerchant должен возвращать общую сумму сделок для мерчанта")
    void findById_ShouldReturnDealSummary_WhenFound() {
        Merchant merchant = Merchant.NEURAL_PAY;
        Deal deal1 = Deal.builder().dealId(1L).merchant(merchant).amount(500).build();
        Deal deal2 = Deal.builder().dealId(2L).merchant(merchant).amount(756).build();
        List<Deal> deals =List.of(deal1, deal2);

        when(dealRepository.findAllByMerchant(merchant)).thenReturn(deals);

        DealSummaryDTO result = dealService.findByMerchant(merchant);

        assertNotNull(result);
        assertEquals(2, result.getDealIds().size());
        assertTrue(result.getDealIds().containsAll(List.of(1L, 2L)));
        assertEquals(1256, result.getTotalAmount());

        verify(dealRepository, times(1)).findAllByMerchant(merchant);
    }

    @Test
    @DisplayName("findByMerchant должен возвращать пустой DTO с нулевой суммой, если сделок не найдено")
    void findByMerchant_ShouldReturnEmptySummary_WhenNoDealsFound() {
        Merchant merchant = Merchant.NEURAL_PAY;
        when(dealRepository.findAllByMerchant(merchant)).thenReturn(Collections.emptyList());

        DealSummaryDTO result = dealService.findByMerchant(merchant);

        assertNotNull(result, "Результат не должен быть null");

        assertEquals(0, result.getTotalAmount(), "Сумма должна быть 0");
        assertNotNull(result.getDealIds(), "Список ID не должен быть null");
        assertTrue(result.getDealIds().isEmpty(), "Список ID должен быть пустым");

        verify(dealRepository).findAllByMerchant(merchant);
    }

    @Test
    @DisplayName("deleteById должен вызывать репозиторий, если тикет существует")
    void deleteById_ShouldExecute_WhenExists() {
        Merchant merchant = Merchant.NEURAL_PAY;
        when(dealRepository.existsByMerchant(merchant)).thenReturn(true);

        dealService.deleteByMerchant(merchant);

        verify(dealRepository).deleteByMerchant(merchant);
    }

    @Test
    @DisplayName("deleteByMerchant должен пробросить исключение BadRequestException, если тикета нет")
    void deleteById_ShouldNotExecute_WhenNotExists() {
        Merchant merchant = Merchant.NEURAL_PAY;
        when(dealRepository.existsByMerchant(merchant)).thenReturn(false);

        assertThatException().isThrownBy(() -> dealService.deleteByMerchant(merchant)).isInstanceOf(BadRequestException.class);
    }
}