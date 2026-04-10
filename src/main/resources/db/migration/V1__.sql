CREATE TABLE users
(
    id       UUID         NOT NULL,
    email    VARCHAR(255) NOT NULL,
    name     VARCHAR(255),
    surname  VARCHAR(255),
    password VARCHAR(255),
    CONSTRAINT pk_users PRIMARY KEY (id)
);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);