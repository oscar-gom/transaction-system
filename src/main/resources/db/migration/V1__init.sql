CREATE TABLE accounts
(
    id         UUID                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_id    UUID                        NOT NULL,
    currency   VARCHAR(3)                  NOT NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (id)
);

CREATE TABLE ledger_lines
(
    id             UUID                        NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    transaction_id UUID                        NOT NULL,
    account_id     UUID                        NOT NULL,
    amount         DECIMAL,
    CONSTRAINT pk_ledger_lines PRIMARY KEY (id)
);

CREATE TABLE transactions
(
    id                  UUID                        NOT NULL,
    created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    idempotency_key     UUID                        NOT NULL,
    receiver_account_id UUID                        NOT NULL,
    sender_account_id   UUID                        NOT NULL,
    amount              DECIMAL(22, 2)              NOT NULL,
    status              VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_transactions PRIMARY KEY (id)
);

CREATE TABLE users
(
    id         UUID                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    email      VARCHAR(255)                NOT NULL,
    name       VARCHAR(255)                NOT NULL,
    surname    VARCHAR(255)                NOT NULL,
    password   VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

ALTER TABLE transactions
    ADD CONSTRAINT uc_transactions_idempotency_key UNIQUE (idempotency_key);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

CREATE INDEX idx_accounts_currency ON accounts (currency);

CREATE INDEX idx_tx_created ON transactions (created_at);

CREATE INDEX idx_tx_idempotency_key ON transactions (idempotency_key);

CREATE INDEX idx_users_email ON users (email);

ALTER TABLE accounts
    ADD CONSTRAINT FK_ACCOUNTS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

CREATE INDEX idx_accounts_user_id ON accounts (user_id);

ALTER TABLE ledger_lines
    ADD CONSTRAINT FK_LEDGER_LINES_ON_ACCOUNT FOREIGN KEY (account_id) REFERENCES accounts (id);

CREATE INDEX idx_account_id ON ledger_lines (account_id);

ALTER TABLE ledger_lines
    ADD CONSTRAINT FK_LEDGER_LINES_ON_TRANSACTION FOREIGN KEY (transaction_id) REFERENCES transactions (id);

CREATE INDEX idx_transaction_id ON ledger_lines (transaction_id);

ALTER TABLE transactions
    ADD CONSTRAINT FK_TRANSACTIONS_ON_RECEIVER_ACCOUNT FOREIGN KEY (receiver_account_id) REFERENCES accounts (id);

CREATE INDEX idx_tx_receiver ON transactions (receiver_account_id);

ALTER TABLE transactions
    ADD CONSTRAINT FK_TRANSACTIONS_ON_SENDER_ACCOUNT FOREIGN KEY (sender_account_id) REFERENCES accounts (id);

CREATE INDEX idx_tx_sender ON transactions (sender_account_id);