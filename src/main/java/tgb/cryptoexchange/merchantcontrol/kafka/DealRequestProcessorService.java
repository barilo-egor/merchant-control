package tgb.cryptoexchange.merchantcontrol.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import tgb.cryptoexchange.merchantcontrol.service.DealService;

@Service
@Slf4j
public class DealRequestProcessorService {

    private final DealService dealService;

    private final ThreadPoolTaskExecutor dealRequestSaveExecutor;

    public DealRequestProcessorService(DealService dealService,
                                       ThreadPoolTaskExecutor dealRequestSaveExecutor) {
        this.dealService = dealService;
        this.dealRequestSaveExecutor = dealRequestSaveExecutor;
    }

    public void process(DealReceive dealRequest) {
        dealRequestSaveExecutor.submit(() -> {
            try {
                dealService.save(dealRequest);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
    }

}
