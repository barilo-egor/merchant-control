package tgb.cryptoexchange.merchantcontrol.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tgb.cryptoexchange.commons.enums.Merchant;
import tgb.cryptoexchange.merchantcontrol.enums.FiatCurrency;

import java.time.Instant;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Deal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long dealId;

    @Column(nullable = false)
    private String appId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Merchant merchant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FiatCurrency fiatCurrency;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    private Instant createDate;

}
