package tgb.cryptoexchange.merchantcontrol.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tgb.cryptoexchange.commons.enums.Merchant;
import tgb.cryptoexchange.merchantcontrol.dto.DealSummaryDTO;
import tgb.cryptoexchange.merchantcontrol.enums.FiatCurrency;
import tgb.cryptoexchange.merchantcontrol.service.DealService;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DealController.class)
class DealControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DealService dealService;

    @Test
    @DisplayName("GET /merchant-control/{merchant} должен возвращать 200 и данные")
    void findByMerchant_ShouldReturnSuccess() throws Exception {
        Merchant merchant = Merchant.ALFA_TEAM;
        DealSummaryDTO summary = DealSummaryDTO.builder()
                .dealIdsByAppId(Map.of("app1", List.of(1L, 2L)))
                .totalAmounts(Map.of(FiatCurrency.RUB, 2000L))
                .build();

        when(dealService.findByMerchant(merchant, null)).thenReturn(summary);

        mockMvc.perform(get("/merchant-control/{merchant}", merchant))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.totalAmounts.RUB").value(2000))
                .andExpect(jsonPath("$.data.dealIdsByAppId.app1").isArray())
                .andExpect(jsonPath("$.data.dealIdsByAppId.app1[0]").value(1))
                .andExpect(jsonPath("$.data.dealIdsByAppId.app1[1]").value(2));

        verify(dealService, times(1)).findByMerchant(merchant, null);
    }

    @Test
    @DisplayName("GET /merchant-control/{merchant} должен выводить суммы раздельно по валютам, рубль — первым, вместо общей суммы")
    void findByMerchant_ShouldReturnTotalAmountsPerCurrency() throws Exception {
        Map<FiatCurrency, Long> totalAmounts = new EnumMap<>(FiatCurrency.class);
        totalAmounts.put(FiatCurrency.KZT, 1250000L);
        totalAmounts.put(FiatCurrency.RUB, 37100L);

        when(dealService.findByMerchant(Merchant.GEO_TRANSFER, null)).thenReturn(
                DealSummaryDTO.builder().totalAmounts(totalAmounts).merchant(Merchant.GEO_TRANSFER).build());

        mockMvc.perform(get("/merchant-control/{merchant}", Merchant.GEO_TRANSFER))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(
                        "\"totalAmounts\":{\"RUB\":37100,\"KZT\":1250000}")))
                .andExpect(jsonPath("$.data.totalAmount").doesNotExist());
    }

    @Test
    @DisplayName("GET /merchant-control?merchants=... должен возвращать список итогов с разбивкой по валютам без totalAmount")
    void findByMerchants_ShouldReturnSummariesList() throws Exception {
        Map<FiatCurrency, Long> totalAmounts = new EnumMap<>(FiatCurrency.class);
        totalAmounts.put(FiatCurrency.RUB, 37100L);
        totalAmounts.put(FiatCurrency.KZT, 1250000L);

        when(dealService.findByMerchants(List.of(Merchant.GEO_TRANSFER, Merchant.ALFA_TEAM))).thenReturn(List.of(
                DealSummaryDTO.builder()
                        .merchant(Merchant.GEO_TRANSFER)
                        .totalAmounts(totalAmounts)
                        .dealIdsByAppId(Map.of("app1", List.of(1L, 2L)))
                        .build()
        ));

        mockMvc.perform(get("/merchant-control")
                        .param("merchants", Merchant.GEO_TRANSFER.name(), Merchant.ALFA_TEAM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].merchant").value(Merchant.GEO_TRANSFER.name()))
                .andExpect(jsonPath("$.data[0].totalAmounts.RUB").value(37100))
                .andExpect(jsonPath("$.data[0].totalAmounts.KZT").value(1250000))
                .andExpect(jsonPath("$.data[0].totalAmount").doesNotExist());

        verify(dealService, times(1)).findByMerchants(List.of(Merchant.GEO_TRANSFER, Merchant.ALFA_TEAM));
    }

    @Test
    @DisplayName("DELETE /merchant-control/{merchant} должен возвращать 200")
    void deleteByMerchant_ShouldReturnOk() throws Exception {
        Merchant merchant = Merchant.ALFA_TEAM;
        Instant dealLastDate = Instant.now();

        mockMvc.perform(delete("/merchant-control/{merchant}", merchant)
                        .param("dealLastDate", dealLastDate.toString()))
                .andExpect(status().isOk());

        verify(dealService, times(1)).deleteByMerchantAndDate(eq(merchant), any(Instant.class));
    }

    @Test
    @DisplayName("GET /merchant-control/{merchant} должен возвращать 400 при невалидном Enum")
    void findByMerchant_ShouldReturnBadRequest_WhenEnumInvalid() throws Exception {
        mockMvc.perform(get("/merchant-control/INVALID_MERCHANT"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(dealService);
    }
}
