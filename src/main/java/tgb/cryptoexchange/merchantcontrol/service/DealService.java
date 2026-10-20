package tgb.cryptoexchange.merchantcontrol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tgb.cryptoexchange.commons.enums.Merchant;
import tgb.cryptoexchange.exception.BadRequestException;
import tgb.cryptoexchange.merchantcontrol.dto.DealSummaryDTO;
import tgb.cryptoexchange.merchantcontrol.entity.Deal;
import tgb.cryptoexchange.merchantcontrol.kafka.DealReceive;
import tgb.cryptoexchange.merchantcontrol.repository.DealRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                .createDate(dealReceive.getCreateDate())
                .build();
        dealRepository.save(deal);
        log.info("Сделка сохранена с ID: {}", deal.getId());
    }


    public DealSummaryDTO findByMerchant(Merchant merchant, Instant dealLastDate) {
        log.debug("Запрос на получение сделок для мерчанта: {}, до даты: {}", merchant, dealLastDate);

        List<Deal> deals = (dealLastDate != null)
                ? dealRepository.findAllByMerchantAndCreateDateLessThanEqual(merchant, dealLastDate)
                : dealRepository.findAllByMerchant(merchant);

        return DealSummaryDTO.fromEntity(merchant, deals);
    }

    public List<DealSummaryDTO> findByMerchants(List<Merchant> merchants) {
        log.debug("Запрос на получение общих сумм сделок для мерчантов: {}", merchants);
        List<Deal> deals = dealRepository.findAllByMerchantIn(merchants);
        Map<Merchant, List<Deal>> dealsByMerchant = deals.stream()
                .collect(Collectors.groupingBy(Deal::getMerchant));

        return merchants.stream()
                .map(merchant -> DealSummaryDTO.fromEntity(
                        merchant,
                        dealsByMerchant.getOrDefault(merchant, List.of())
                ))
                .toList();
    }

    public void deleteByMerchantAndDate(Merchant merchant, Instant dealLastDate) {
        log.info("Запрос на удаление всех сделок мерчанта: {} до {}", merchant, dealLastDate);
        if (dealRepository.existsByMerchantAndCreateDateLessThanEqual(merchant, dealLastDate)) {
            dealRepository.deleteByMerchantAndCreateDateLessThanEqual(merchant, dealLastDate);
            log.info("Удалены сделки для мерчанта = {} до {}", merchant, dealLastDate);
        } else {
            log.warn("Сделок для мерчанта = {} до {} не существует.", merchant, dealLastDate);
            throw new BadRequestException(String.format("Deal with merchant %s before create date %s not found.", merchant, dealLastDate));
        }
    }

}
