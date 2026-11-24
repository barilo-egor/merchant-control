package tgb.cryptoexchange.merchantcontrol.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tgb.cryptoexchange.commons.enums.Merchant;
import tgb.cryptoexchange.merchantcontrol.enums.FiatCurrency;
import tgb.cryptoexchange.merchantcontrol.projection.MerchantDealIdProjection;
import tgb.cryptoexchange.merchantcontrol.projection.MerchantDealTotalProjection;

import java.time.Instant;
import java.util.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DealSummaryDTO {

    @Builder.Default
    private Map<String, List<Long>> dealIdsByAppId = new LinkedHashMap<>();

    @Builder.Default
    private Map<FiatCurrency, Long> totalAmounts = new EnumMap<>(FiatCurrency.class);

    @JsonSerialize(using = InstantSerializer.class)
    private Instant dealLastDate;

    private Merchant merchant;

    private String appId;

    public static DealSummaryDTO fromProjections(Merchant merchant,
                                                 List<MerchantDealTotalProjection> totals,
                                                 List<MerchantDealIdProjection> dealIds) {
        Map<FiatCurrency, Long> totalAmounts = new EnumMap<>(FiatCurrency.class);
        Instant lastDate = null;

        for (MerchantDealTotalProjection total : totals) {
            if (Objects.nonNull(total.getFiatCurrency())
                    && Objects.nonNull(total.getTotalAmount())
                    && total.getTotalAmount() > 0) {
                totalAmounts.put(total.getFiatCurrency(), total.getTotalAmount());
            }

            Instant currencyLastDate = total.getDealLastDate();
            if (Objects.nonNull(currencyLastDate) && (Objects.isNull(lastDate) || currencyLastDate.isAfter(lastDate))) {
                lastDate = currencyLastDate;
            }
        }

        Map<String, List<Long>> idsByAppId = new LinkedHashMap<>();
        for (MerchantDealIdProjection dealId : dealIds) {
            idsByAppId.computeIfAbsent(dealId.getAppId(), k -> new ArrayList<>())
                    .add(dealId.getDealId());
        }

        return DealSummaryDTO.builder()
                .dealIdsByAppId(idsByAppId)
                .totalAmounts(totalAmounts)
                .dealLastDate(lastDate)
                .merchant(merchant)
                .build();
    }

}
