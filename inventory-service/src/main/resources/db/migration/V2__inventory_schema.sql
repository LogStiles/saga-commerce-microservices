CREATE TABLE IF NOT EXISTS inventory
(
    id            INT PRIMARY KEY,
    creation_time TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    name          VARCHAR(255),
    price         DECIMAL(10, 2) NOT NULL,
    stock_amount  INT            NOT NULL
);
