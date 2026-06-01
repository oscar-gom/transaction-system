CREATE TABLE contacts
(
    id                 UUID                        NOT NULL,
    created_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    owner_user_id      UUID                        NOT NULL,
    contact_account_id UUID                        NOT NULL,
    alias              VARCHAR(60)                 NOT NULL,
    note               VARCHAR(255),
    favorite           BOOLEAN       DEFAULT FALSE NOT NULL,
    CONSTRAINT pk_contacts PRIMARY KEY (id)
);

ALTER TABLE contacts
    ADD CONSTRAINT fk_contacts_on_owner FOREIGN KEY (owner_user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE contacts
    ADD CONSTRAINT fk_contacts_on_account FOREIGN KEY (contact_account_id) REFERENCES accounts (id) ON DELETE CASCADE;

ALTER TABLE contacts
    ADD CONSTRAINT uc_contacts_owner_target UNIQUE (owner_user_id, contact_account_id);

CREATE INDEX idx_contacts_owner ON contacts (owner_user_id);

CREATE INDEX idx_contacts_owner_favorite ON contacts (owner_user_id, favorite);
