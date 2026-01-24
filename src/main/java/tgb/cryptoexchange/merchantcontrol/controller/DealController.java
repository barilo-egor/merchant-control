package tgb.cryptoexchange.merchantcontrol.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tgb.cryptoexchange.commons.enums.Merchant;
import tgb.cryptoexchange.controller.ApiController;
import tgb.cryptoexchange.merchantcontrol.dto.DealSummaryDTO;
import tgb.cryptoexchange.merchantcontrol.service.DealService;
import tgb.cryptoexchange.web.ApiResponse;

import java.time.Instant;

@RestController
@RequestMapping("/merchant-control")
@Slf4j
public class DealController extends ApiController {

    private final DealService dealService;

    public DealController(DealService dealService) {
        this.dealService = dealService;
    }

    @GetMapping("/{merchant}")
    public ResponseEntity<ApiResponse<DealSummaryDTO>> findByMerchant(@PathVariable Merchant merchant) {
        return new ResponseEntity<>(ApiResponse.success(
                dealService.findByMerchant(merchant)),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{merchant}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteByMerchant(@PathVariable Merchant merchant, @RequestParam Instant dealLastDate) {
        dealService.deleteByMerchantAndDate(merchant, dealLastDate);
    }

}
