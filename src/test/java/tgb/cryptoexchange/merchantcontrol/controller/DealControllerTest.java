package tgb.cryptoexchange.merchantcontrol.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tgb.cryptoexchange.commons.enums.Merchant;
import tgb.cryptoexchange.merchantcontrol.dto.DealSummaryDTO;
import tgb.cryptoexchange.merchantcontrol.service.DealService;

import java.time.Instant;
import java.util.List;

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
        Merchant merchant = Merchant.NEURAL_PAY;
        DealSummaryDTO summary = DealSummaryDTO.builder()
                .dealIds(List.of(1L, 2L))
                .totalAmount(2000)
                .build();

        when(dealService.findByMerchant(merchant)).thenReturn(summary);

        mockMvc.perform(get("/merchant-control/{merchant}", merchant))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.totalAmount").value(2000))
                .andExpect(jsonPath("$.data.dealIds").isArray())
                .andExpect(jsonPath("$.data.dealIds[0]").value(1));

        verify(dealService, times(1)).findByMerchant(merchant);
    }

    @Test
    @DisplayName("DELETE /merchant-control/{merchant} должен возвращать 200")
    void deleteByMerchant_ShouldReturnOk() throws Exception {
        Merchant merchant = Merchant.NEURAL_PAY;
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