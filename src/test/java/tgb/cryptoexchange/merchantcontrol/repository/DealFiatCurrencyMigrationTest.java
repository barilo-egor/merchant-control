package tgb.cryptoexchange.merchantcontrol.repository;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class DealFiatCurrencyMigrationTest {

    private static final String URL = "jdbc:h2:mem:deal-fiat-currency-migration;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    private static final String INSERT_DEAL_WITHOUT_CURRENCY = """
            insert into deal (amount, app_id, create_date, deal_id, merchant)
            values (?, ?, ?, ?, ?)
            """;

    private static void insertDealWithoutCurrency(JdbcTemplate jdbcTemplate, Long dealId, Integer amount) {
        jdbcTemplate.update(INSERT_DEAL_WITHOUT_CURRENCY,
                amount, "TG", Timestamp.from(Instant.now()), dealId, "ALFA_TEAM");
    }

    @Test
    @DisplayName("Миграция V2 должна проставлять существующим сделкам RUB и запрещать NULL без значения по умолчанию")
    void migration_ShouldSetRubToExistingDealsAndRequireNotNull() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, "sa", "");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Flyway.configure()
                .dataSource(dataSource)
                .target("1")
                .load()
                .migrate();
        insertDealWithoutCurrency(jdbcTemplate, 1L, 1000);
        insertDealWithoutCurrency(jdbcTemplate, 2L, 2000);

        Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate();

        assertThat(jdbcTemplate.queryForList("select fiat_currency from deal order by deal_id", String.class))
                .containsExactly("RUB", "RUB");
        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> insertDealWithoutCurrency(jdbcTemplate, 3L, 3000));
    }

}
