CREATE TABLE product_snapshot
(
    id         BIGINT       NOT NULL,
    name       VARCHAR(255) NOT NULL,
    price      INT          NOT NULL,
    discount   INT          NOT NULL,
    CONSTRAINT pk_product_snapshot PRIMARY KEY (id)
);