package tgb.cryptoexchange.merchantcontrol.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DealRequestTopicListenerTest {

    @Mock
    private DealRequestProcessorService dealRequestProcessorService;

    @InjectMocks
    private DealRequestTopicListener dealRequestTopicListener;

    @Test
    void receiveShouldCallServiceMethodWithAllMerchants() {
        DealReceive detailsRequest = new DealReceive();
        detailsRequest.setDealId(1L);
        dealRequestTopicListener.receive(detailsRequest);
        verify(dealRequestProcessorService).process(detailsRequest);
    }

}