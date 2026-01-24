package tgb.cryptoexchange.merchantcontrol.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tgb.cryptoexchange.commons.enums.Merchant;
import tgb.cryptoexchange.merchantcontrol.entity.Deal;

import java.time.Instant;
import java.util.List;

public interface DealRepository extends JpaRepository<Deal, Long>, JpaSpecificationExecutor<Deal> {

    List<Deal> findAllByMerchant(Merchant merchant);

    boolean existsByMerchantAndCreateDateGreaterThanEqual(Merchant merchant, Instant createDate);


    @Transactional
    void deleteByMerchantAndCreateDateGreaterThanEqual(Merchant merchant, Instant createDate);

}