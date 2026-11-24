ALTER TABLE deal
    ADD COLUMN fiat_currency VARCHAR(255) NOT NULL DEFAULT 'RUB';

ALTER TABLE deal
    ALTER COLUMN fiat_currency DROP DEFAULT;

CREATE INDEX idx_deal_merchant_create_date ON deal (merchant, create_date);
