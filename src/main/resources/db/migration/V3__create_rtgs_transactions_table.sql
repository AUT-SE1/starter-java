CREATE TABLE rtgs_transactions (
    id VARCHAR(255) PRIMARY KEY,
    source_iban VARCHAR(255),
    destination_iban VARCHAR(255),
    amount DECIMAL(19, 2),
    description VARCHAR(255),
    status SMALLINT,
    failure_reason VARCHAR(255)
);