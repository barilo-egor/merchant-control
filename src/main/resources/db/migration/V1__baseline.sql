CREATE TABLE deal
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    amount      INT          NOT NULL,
    app_id      VARCHAR(255) NOT NULL,
    create_date DATETIME(6)  NOT NULL,
    deal_id     BIGINT       NOT NULL,
    merchant    VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);
