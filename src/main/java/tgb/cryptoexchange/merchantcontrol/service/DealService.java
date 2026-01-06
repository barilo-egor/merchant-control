package tgb.cryptoexchange.merchantcontrol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tgb.cryptoexchange.commons.enums.Merchant;
import tgb.cryptoexchange.exception.BadRequestException;
import tgb.cryptoexchange.merchantcontrol.dto.DealSummaryDTO;
import tgb.cryptoexchange.merchantcontrol.entity.Deal;
import tgb.cryptoexchange.merchantcontrol.kafka.DealReceive;
import tgb.cryptoexchange.merchantcontrol.repository.DealRepository;

import java.util.List;

@Service
@Slf4j
public class DealService {

    private final DealRepository dealRepository;

    public DealService(DealRepository dealRepository) {
        this.dealRepository = dealRepository;
    }

    public void save(DealReceive dealReceive) {
        log.info("Запрос на сохранение сделки от мерчанта: {}", dealReceive.getMerchant());
        Deal deal = Deal.builder()
                .dealId(dealReceive.getDealId())
                .appId(dealReceive.getAppId())
                .merchant(dealReceive.getMerchant())
                .amount(dealReceive.getAmount())
                .build();
        dealRepository.save(deal);
        log.info("Сделка сохранена с ID: {}", deal.getId());
    }


    public DealSummaryDTO findByMerchant(Merchant merchant) {
        log.debug("Запрос на получение общей суммы сделок для мерчанта: {}", merchant);
        List<Deal> deals = dealRepository.findAllByMerchant(merchant);
        return DealSummaryDTO.fromEntity(deals);
    }

    public void deleteByMerchant(Merchant merchant) {
        log.info("Запрос на удаление всех сделок мерчанта: {}", merchant);
        if (dealRepository.existsByMerchant(merchant)) {
            dealRepository.deleteByMerchant(merchant);
            log.info("Удалены сделки для мерчанта = {}", merchant);
        } else {
            log.warn("Сделок для мерчанта = {} не существует.", merchant);
            throw new BadRequestException(String.format("Deal with merchant %s not found.", merchant));
        }
    }

}
