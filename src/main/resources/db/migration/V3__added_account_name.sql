ALTER TABLE accounts
    ADD account_name VARCHAR(100);

ALTER TABLE accounts
    ALTER COLUMN account_name SET NOT NULL;

ALTER TABLE accounts
    ADD CONSTRAINT uc_accounts_account_name UNIQUE (account_name);

ALTER TABLE accounts
    ADD CONSTRAINT uc_accounts_user UNIQUE (user_id);

CREATE INDEX idx_accounts_account_name ON accounts (account_name);
