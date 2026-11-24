package tgb.cryptoexchange.merchantcontrol.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tgb.cryptoexchange.commons.enums.Merchant;
import tgb.cryptoexchange.merchantcontrol.entity.Deal;
import tgb.cryptoexchange.merchantcontrol.projection.MerchantDealIdProjection;
import tgb.cryptoexchange.merchantcontrol.projection.MerchantDealTotalProjection;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface DealRepository extends JpaRepository<Deal, Long>, JpaSpecificationExecutor<Deal> {

    List<MerchantDealIdProjection> findAllByMerchantInOrderByAppIdAscDealIdAsc(Collection<Merchant> merchants);

    List<MerchantDealIdProjection> findAllByMerchantInAndCreateDateLessThanEqualOrderByAppIdAscDealIdAsc(Collection<Merchant> merchants, Instant createDate);

    @Query("""
            select d.merchant as merchant, d.fiatCurrency as fiatCurrency,
                   sum(d.amount) as totalAmount, max(d.createDate) as dealLastDate
            from Deal d
            where d.merchant in :merchants
            group by d.merchant, d.fiatCurrency
            """)
    List<MerchantDealTotalProjection> findTotalsByMerchantIn(@Param("merchants") Collection<Merchant> merchants);

    @Query("""
            select d.merchant as merchant, d.fiatCurrency as fiatCurrency,
                   sum(d.amount) as totalAmount, max(d.createDate) as dealLastDate
            from Deal d
            where d.merchant in :merchants and d.createDate <= :dealLastDate
            group by d.merchant, d.fiatCurrency
            """)
    List<MerchantDealTotalProjection> findTotalsByMerchantInAndCreateDateLessThanEqual(@Param("merchants") Collection<Merchant> merchants,
                                                                                       @Param("dealLastDate") Instant dealLastDate);

    boolean existsByMerchantAndCreateDateLessThanEqual(Merchant merchant, Instant createDate);

    @Transactional
    void deleteByMerchantAndCreateDateLessThanEqual(Merchant merchant, Instant createDate);

}
