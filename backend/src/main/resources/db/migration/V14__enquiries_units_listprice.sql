-- V14: supplier enquiries, product units (NOS / LTR), 3-decimal list price

-- 1. UNITS ---------------------------------------------------------------
ALTER TABLE products      ADD COLUMN unit VARCHAR(10) NOT NULL DEFAULT 'NOS';
ALTER TABLE products      ADD CONSTRAINT chk_products_unit CHECK (unit IN ('NOS', 'LTR'));
ALTER TABLE invoice_items ADD COLUMN unit VARCHAR(10) NOT NULL DEFAULT 'NOS';

-- 2. LIST PRICE: 3 decimals ---------------------------------------------
ALTER TABLE products ALTER COLUMN list_price TYPE NUMERIC(13,3);

-- 3. SUPPLIER ENQUIRIES --------------------------------------------------
CREATE TABLE enquiry_counters (
    shop_id        BIGINT      NOT NULL REFERENCES shops (id),
    financial_year VARCHAR(7)  NOT NULL,
    last_number    INTEGER     NOT NULL DEFAULT 0,
    PRIMARY KEY (shop_id, financial_year)
);

CREATE TABLE purchase_enquiries (
    id            BIGSERIAL    PRIMARY KEY,
    shop_id       BIGINT       NOT NULL REFERENCES shops (id),
    enquiry_no    VARCHAR(30)  NOT NULL,
    enquiry_date  DATE         NOT NULL,
    supplier_id   BIGINT       REFERENCES suppliers (id),
    supplier_name VARCHAR(150),
    notes         TEXT,
    created_by    BIGINT,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_enquiries_shop_no UNIQUE (shop_id, enquiry_no)
);
CREATE INDEX idx_enquiries_shop_date ON purchase_enquiries (shop_id, enquiry_date);

CREATE TABLE purchase_enquiry_items (
    id            BIGSERIAL    PRIMARY KEY,
    shop_id       BIGINT       NOT NULL REFERENCES shops (id),
    enquiry_id    BIGINT       NOT NULL REFERENCES purchase_enquiries (id) ON DELETE CASCADE,
    line_no       INTEGER      NOT NULL,
    product_id    BIGINT       REFERENCES products (id),
    part_number   VARCHAR(60),
    description   VARCHAR(200) NOT NULL,
    quantity      INTEGER      NOT NULL CHECK (quantity > 0),
    unit          VARCHAR(10)  NOT NULL DEFAULT 'NOS'
);
CREATE INDEX idx_enquiry_items_shop ON purchase_enquiry_items (shop_id);
CREATE INDEX idx_enquiry_items_enq  ON purchase_enquiry_items (enquiry_id);
