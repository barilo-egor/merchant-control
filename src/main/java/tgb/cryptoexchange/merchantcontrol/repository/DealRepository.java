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

    List<Deal> findAllByMerchantAndCreateDateLessThanEqual(Merchant merchant, Instant createDate);

    boolean existsByMerchantAndCreateDateLessThanEqual(Merchant merchant, Instant createDate);

    List<Deal> findAllByMerchantIn(List<Merchant> merchants);

    @Transactional
    void deleteByMerchantAndCreateDateLessThanEqual(Merchant merchant, Instant createDate);

}