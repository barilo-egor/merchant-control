package tgb.cryptoexchange.merchantcontrol.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;
import tgb.cryptoexchange.merchantcontrol.entity.Deal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DealSummaryDTO {

    @Builder.Default
    private List<Long> dealIds = new ArrayList<>();

    private Integer totalAmount;

    @JsonSerialize(using = InstantSerializer.class)
    private Instant dealLastDate;

    public static DealSummaryDTO fromEntity(List<Deal> deals) {
        if (CollectionUtils.isEmpty(deals)) {
            return DealSummaryDTO.builder()
                    .dealIds(Collections.emptyList())
                    .totalAmount(0)
                    .build();
        }

        List<Long> ids = new ArrayList<>(deals.size());
        int totalAmount = 0;
        Instant lastDate = null;
        for (Deal deal : deals) {
            ids.add(deal.getDealId());
            if (deal.getAmount() != null) {
                totalAmount += deal.getAmount();
            }
            Instant currentDate = deal.getCreateDate();
            if (currentDate != null && (lastDate == null || currentDate.isAfter(lastDate))) {
                    lastDate = currentDate;
                }

        }
        return DealSummaryDTO.builder()
                .dealIds(ids)
                .totalAmount(totalAmount)
                .dealLastDate(lastDate)
                .build();

    }

}
