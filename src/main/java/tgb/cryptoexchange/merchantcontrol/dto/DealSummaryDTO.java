package tgb.cryptoexchange.merchantcontrol.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;
import tgb.cryptoexchange.commons.enums.Merchant;
import tgb.cryptoexchange.merchantcontrol.entity.Deal;

import java.time.Instant;
import java.util.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DealSummaryDTO {

    @Builder.Default
    private Map<String, List<Long>> dealIdsByAppId = new HashMap<>();

    private Integer totalAmount;

    @JsonSerialize(using = InstantSerializer.class)
    private Instant dealLastDate;

    private Merchant merchant;

    private String appId;

    public static DealSummaryDTO fromEntity(Merchant merchant, List<Deal> deals) {
        if (CollectionUtils.isEmpty(deals)) {
            return DealSummaryDTO.builder()
                    .dealIdsByAppId(Collections.emptyMap())
                    .merchant(merchant)
                    .totalAmount(0)
                    .build();
        }

        Map<String, List<Long>> idsByAppId = new HashMap<>();
        int totalAmount = 0;
        Instant lastDate = null;

        for (Deal deal : deals) {
            if (Objects.nonNull(deal.getAppId()) && Objects.nonNull(deal.getDealId())) {
                idsByAppId.computeIfAbsent(deal.getAppId(), k -> new ArrayList<>())
                        .add(deal.getDealId());
            }

            if (Objects.nonNull(deal.getAmount())) {
                totalAmount += deal.getAmount();
            }

            Instant currentDate = deal.getCreateDate();
            if (Objects.nonNull(currentDate) && (Objects.isNull(lastDate) || currentDate.isAfter(lastDate))) {
                lastDate = currentDate;
            }
        }

        return DealSummaryDTO.builder()
                .dealIdsByAppId(idsByAppId)
                .totalAmount(totalAmount)
                .dealLastDate(lastDate)
                .merchant(merchant)
                .build();

    }

}
