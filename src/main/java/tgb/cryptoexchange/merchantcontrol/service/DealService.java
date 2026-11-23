package tgb.cryptoexchange.merchantcontrol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import tgb.cryptoexchange.commons.enums.Merchant;
import tgb.cryptoexchange.exception.BadRequestException;
import tgb.cryptoexchange.merchantcontrol.dto.DealSummaryDTO;
import tgb.cryptoexchange.merchantcontrol.entity.Deal;
import tgb.cryptoexchange.merchantcontrol.kafka.DealReceive;
import tgb.cryptoexchange.merchantcontrol.projection.MerchantDealIdProjection;
import tgb.cryptoexchange.merchantcontrol.projection.MerchantDealTotalProjection;
import tgb.cryptoexchange.merchantcontrol.repository.DealRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DealService {

    private final DealRepository dealRepository;

    public DealService(DealRepository dealRepository) {
        this.dealRepository = dealRepository;
    }

    public void save(DealReceive dealReceive) {
        log.info("Запрос на сохранение сделки от мерчанта: {}, dealId={}, fiatCurrency={}, amount={}",
                dealReceive.getMerchant(), dealReceive.getDealId(), dealReceive.getFiatCurrency(), dealReceive.getAmount());
        Assert.notNull(dealReceive.getFiatCurrency(), () -> String.format(
                "Не указана фиатная валюта сделки: dealId=%s, appId=%s", dealReceive.getDealId(), dealReceive.getAppId()));
        Deal deal = Deal.builder()
                .dealId(dealReceive.getDealId())
                .appId(dealReceive.getAppId())
                .merchant(dealReceive.getMerchant())
                .fiatCurrency(dealReceive.getFiatCurrency())
                .amount(dealReceive.getAmount())
                .createDate(dealReceive.getCreateDate())
                .build();
        dealRepository.save(deal);
        log.info("Сделка сохранена с ID: {}", deal.getId());
    }

    @Transactional(readOnly = true)
    public DealSummaryDTO findByMerchant(Merchant merchant, Instant dealLastDate) {
        log.debug("Запрос на получение сделок для мерчанта: {}, до даты: {}", merchant, dealLastDate);
        return getSummaries(List.of(merchant), dealLastDate).getFirst();
    }

    @Transactional(readOnly = true)
    public List<DealSummaryDTO> findByMerchants(List<Merchant> merchants) {
        log.debug("Запрос на получение общих сумм сделок для мерчантов: {}", merchants);
        if (CollectionUtils.isEmpty(merchants)) {
            return List.of();
        }
        return getSummaries(merchants, null).stream()
                .filter(summary -> !summary.getTotalAmounts().isEmpty())
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

    private List<DealSummaryDTO> getSummaries(List<Merchant> merchants, Instant dealLastDate) {
        Map<Merchant, List<MerchantDealTotalProjection>> totalsByMerchant = groupByMerchant(loadTotals(merchants, dealLastDate),
                MerchantDealTotalProjection::getMerchant);
        Map<Merchant, List<MerchantDealIdProjection>> dealIdsByMerchant = groupByMerchant(loadDealIds(merchants, dealLastDate),
                MerchantDealIdProjection::getMerchant);

        return merchants.stream()
                .map(merchant -> DealSummaryDTO.fromProjections(
                        merchant,
                        totalsByMerchant.getOrDefault(merchant, List.of()),
                        dealIdsByMerchant.getOrDefault(merchant, List.of())))
                .toList();
    }

    private List<MerchantDealTotalProjection> loadTotals(List<Merchant> merchants, Instant dealLastDate) {
        return (dealLastDate != null)
                ? dealRepository.findTotalsByMerchantInAndCreateDateLessThanEqual(merchants, dealLastDate)
                : dealRepository.findTotalsByMerchantIn(merchants);
    }

    private List<MerchantDealIdProjection> loadDealIds(List<Merchant> merchants, Instant dealLastDate) {
        return (dealLastDate != null)
                ? dealRepository.findAllByMerchantInAndCreateDateLessThanEqualOrderByAppIdAscDealIdAsc(merchants, dealLastDate)
                : dealRepository.findAllByMerchantInOrderByAppIdAscDealIdAsc(merchants);
    }

    private static <T> Map<Merchant, List<T>> groupByMerchant(List<T> projections, Function<T, Merchant> merchantGetter) {
        return projections.stream()
                .collect(Collectors.groupingBy(merchantGetter));
    }

}
