ALTER TABLE users
    ADD account_type VARCHAR(255);

ALTER TABLE users
    ALTER COLUMN account_type SET NOT NULL;
