ALTER TABLE accounts
    ADD account_name VARCHAR(100);

UPDATE accounts
SET account_name = user_id::VARCHAR(100)
WHERE account_name IS NULL;

ALTER TABLE accounts
    ALTER COLUMN account_name SET NOT NULL;

ALTER TABLE accounts
    ADD CONSTRAINT uc_accounts_account_name UNIQUE (account_name);

CREATE INDEX idx_accounts_account_name ON accounts (account_name);
