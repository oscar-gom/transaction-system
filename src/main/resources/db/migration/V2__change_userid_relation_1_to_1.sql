ALTER TABLE accounts
    ADD CONSTRAINT uc_accounts_user_id UNIQUE (user_id);
