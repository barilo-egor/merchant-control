package tgb.cryptoexchange.merchantcontrol.kafka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tgb.cryptoexchange.merchantcontrol.service.DealService;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = DealRequestProcessorService.class)
@Import(DealRequestProcessorServiceTest.Config.class)
class DealRequestProcessorServiceTest {

    @Autowired
    private DealRequestProcessorService processorService;

    @MockitoBean
    private DealService dealService;

    @TestConfiguration
    static class Config {
        @Bean
        public ThreadPoolTaskExecutor dealRequestSaveExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.initialize();
            return executor;
        }
    }

    @Test
    @DisplayName("process должен асинхронно сохранить тикет")
    void process_ShouldSaveAsynchronously() {
        DealReceive request = new DealReceive();

        processorService.process(request);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(dealService).save(request);
        });
    }

}
