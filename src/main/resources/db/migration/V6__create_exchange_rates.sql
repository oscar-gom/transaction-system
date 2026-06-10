CREATE TABLE exchange_rates (
    base_currency VARCHAR(3) PRIMARY KEY,
    last_updated TIMESTAMP NOT NULL
);

CREATE TABLE exchange_rate_values (
    base_currency VARCHAR(3) NOT NULL,
    target_currency VARCHAR(3) NOT NULL,
    rate NUMERIC(20, 10) NOT NULL,
    PRIMARY KEY (base_currency, target_currency),
    CONSTRAINT fk_base_currency FOREIGN KEY (base_currency) REFERENCES exchange_rates(base_currency) ON DELETE CASCADE
);
