package tgb.cryptoexchange.merchantcontrol.projection;

import tgb.cryptoexchange.commons.enums.Merchant;
import tgb.cryptoexchange.merchantcontrol.enums.FiatCurrency;

import java.time.Instant;

public interface MerchantDealTotalProjection {

    Merchant getMerchant();

    FiatCurrency getFiatCurrency();

    Long getTotalAmount();

    Instant getDealLastDate();

}
