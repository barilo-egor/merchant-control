package tgb.cryptoexchange.merchantcontrol.projection;

import tgb.cryptoexchange.commons.enums.Merchant;

public interface MerchantDealIdProjection {

    Merchant getMerchant();

    String getAppId();

    Long getDealId();

}
