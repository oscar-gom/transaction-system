ALTER TABLE users
    ADD account_type VARCHAR(255);

UPDATE users
    SET account_type = 'USER'
    WHERE account_type IS NULL;

ALTER TABLE users
    ALTER COLUMN account_type SET NOT NULL;
