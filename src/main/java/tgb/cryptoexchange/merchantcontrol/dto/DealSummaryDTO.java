package tgb.cryptoexchange.merchantcontrol.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;
import tgb.cryptoexchange.merchantcontrol.entity.Deal;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DealSummaryDTO {

    private List<Long> dealIds;

    private Integer totalAmount;

    public static DealSummaryDTO fromEntity(List<Deal> deals) {
        if (CollectionUtils.isEmpty(deals)) {
            return DealSummaryDTO.builder()
                    .dealIds(Collections.emptyList())
                    .totalAmount(0)
                    .build();
        }
        List<Long> ids = deals.stream()
                .map(Deal::getDealId)
                .toList();
        Integer totalAmount = deals.stream()
                .map(Deal::getAmount)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum);
        return DealSummaryDTO.builder()
                .dealIds(ids)
                .totalAmount(totalAmount)
                .build();

    }

}
