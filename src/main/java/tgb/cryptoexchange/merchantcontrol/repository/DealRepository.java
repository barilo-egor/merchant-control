package tgb.cryptoexchange.merchantcontrol.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tgb.cryptoexchange.commons.enums.Merchant;
import tgb.cryptoexchange.merchantcontrol.entity.Deal;

import java.util.List;

public interface DealRepository extends JpaRepository<Deal, Long>, JpaSpecificationExecutor<Deal> {

    List<Deal> findAllByMerchant(Merchant merchant);

    boolean existsByMerchant(Merchant merchant);

    void deleteByMerchant(Merchant merchant);

}
