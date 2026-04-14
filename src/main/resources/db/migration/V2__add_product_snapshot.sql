CREATE TABLE product_snapshot
(
    product_id BIGINT       NOT NULL,
    name       VARCHAR(255) NULL,
    price      INT          NOT NULL,
    discount   INT          NOT NULL,
    CONSTRAINT pk_product_snapshot PRIMARY KEY (product_id)
);