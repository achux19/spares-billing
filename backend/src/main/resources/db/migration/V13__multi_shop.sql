-- V13: multi-shop support (Super Admin release)
-- Every business row now belongs to a shop. All existing data becomes shop 1.

-- 1. SHOPS ---------------------------------------------------------------
CREATE TABLE shops (
                       id         BIGSERIAL    PRIMARY KEY,
                       code       VARCHAR(30)  NOT NULL,
                       name       VARCHAR(150) NOT NULL,
                       status     VARCHAR(12)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED')),
                       created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
                       CONSTRAINT chk_shops_code CHECK (code ~ '^[a-z0-9][a-z0-9-]{1,29}$')
    );
CREATE UNIQUE INDEX uq_shops_code ON shops (code);

-- the existing business becomes shop 1 (code can be changed later by Super Admin)
INSERT INTO shops (id, code, name)
VALUES (1, 'main', COALESCE((SELECT shop_name FROM shop_settings ORDER BY id LIMIT 1), 'My Auto Spares'));
SELECT setval(pg_get_serial_sequence('shops', 'id'), 1);

-- 2. FEATURE SWITCHES ----------------------------------------------------
CREATE TABLE shop_features (
                               shop_id     BIGINT      NOT NULL REFERENCES shops (id),
                               feature_key VARCHAR(40) NOT NULL,
                               enabled     BOOLEAN     NOT NULL DEFAULT TRUE,
                               PRIMARY KEY (shop_id, feature_key)
);
INSERT INTO shop_features (shop_id, feature_key)
SELECT 1, k FROM unnest(ARRAY['PURCHASES', 'QUOTATIONS', 'CREDIT_NOTES', 'EWAY', 'REPORTS']) AS k;

-- 3. shop_id ON EVERY BUSINESS TABLE -------------------------------------
DO $$
DECLARE t text;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'categories', 'brands', 'products', 'customers', 'suppliers',
        'purchases', 'purchase_items', 'purchase_payments',
        'invoices', 'invoice_items', 'invoice_counters', 'payments', 'stock_movements',
        'credit_notes', 'credit_note_items', 'credit_note_counters',
        'quotations', 'quotation_items', 'quotation_counters',
        'eway_details', 'shop_settings', 'branding'
    ] LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN shop_id BIGINT REFERENCES shops (id)', t);
EXECUTE format('UPDATE %I SET shop_id = 1', t);
EXECUTE format('ALTER TABLE %I ALTER COLUMN shop_id SET NOT NULL', t);
EXECUTE format('CREATE INDEX %I ON %I (shop_id)', 'idx_' || t || '_shop', t);
END LOOP;
END $$;

-- 4. USERS: belong to a shop, except the platform Super Admin ------------
ALTER TABLE users ADD COLUMN shop_id BIGINT REFERENCES shops (id);
UPDATE users SET shop_id = 1;
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP;
ALTER TABLE users DROP CONSTRAINT users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('SUPER_ADMIN', 'OWNER', 'STAFF'));
ALTER TABLE users ADD CONSTRAINT chk_users_shop_role CHECK ((role = 'SUPER_ADMIN') = (shop_id IS NULL));
ALTER TABLE users DROP CONSTRAINT users_username_key;
CREATE UNIQUE INDEX uq_users_shop_username ON users (shop_id, username) WHERE shop_id IS NOT NULL;
CREATE UNIQUE INDEX uq_users_platform_username ON users (username) WHERE shop_id IS NULL;
CREATE INDEX idx_users_shop ON users (shop_id);

-- 5. UNIQUE RULES BECOME PER SHOP ----------------------------------------
ALTER TABLE categories DROP CONSTRAINT categories_name_key;
ALTER TABLE categories ADD CONSTRAINT uq_categories_shop_name UNIQUE (shop_id, name);
ALTER TABLE brands DROP CONSTRAINT brands_name_key;
ALTER TABLE brands ADD CONSTRAINT uq_brands_shop_name UNIQUE (shop_id, name);

DROP INDEX uq_products_part_brand;
CREATE UNIQUE INDEX uq_products_part_brand ON products (shop_id, LOWER(part_number), COALESCE(brand_id, 0));

DROP INDEX uq_customers_gstin;
CREATE UNIQUE INDEX uq_customers_gstin ON customers (shop_id, gstin) WHERE gstin IS NOT NULL;
DROP INDEX uq_suppliers_gstin;
CREATE UNIQUE INDEX uq_suppliers_gstin ON suppliers (shop_id, gstin) WHERE gstin IS NOT NULL;

ALTER TABLE invoices DROP CONSTRAINT invoices_invoice_no_key;
ALTER TABLE invoices ADD CONSTRAINT uq_invoices_shop_no UNIQUE (shop_id, invoice_no);
ALTER TABLE credit_notes DROP CONSTRAINT credit_notes_credit_note_no_key;
ALTER TABLE credit_notes ADD CONSTRAINT uq_credit_notes_shop_no UNIQUE (shop_id, credit_note_no);
ALTER TABLE quotations DROP CONSTRAINT quotations_quotation_no_key;
ALTER TABLE quotations ADD CONSTRAINT uq_quotations_shop_no UNIQUE (shop_id, quotation_no);

-- 6. COUNTERS: one counter per shop per financial year -------------------
ALTER TABLE invoice_counters     DROP CONSTRAINT invoice_counters_pkey;
ALTER TABLE invoice_counters     ADD PRIMARY KEY (shop_id, financial_year);
ALTER TABLE credit_note_counters DROP CONSTRAINT credit_note_counters_pkey;
ALTER TABLE credit_note_counters ADD PRIMARY KEY (shop_id, financial_year);
ALTER TABLE quotation_counters   DROP CONSTRAINT quotation_counters_pkey;
ALTER TABLE quotation_counters   ADD PRIMARY KEY (shop_id, financial_year);

-- 7. SINGLE-ROW TABLES BECOME ONE ROW PER SHOP (row id = shop id) --------
ALTER TABLE shop_settings DROP CONSTRAINT shop_settings_id_check;
ALTER TABLE shop_settings ALTER COLUMN id DROP DEFAULT;
ALTER TABLE shop_settings ADD CONSTRAINT uq_shop_settings_shop UNIQUE (shop_id);
ALTER TABLE branding DROP CONSTRAINT branding_id_check;
ALTER TABLE branding ADD CONSTRAINT uq_branding_shop UNIQUE (shop_id);

-- 8. useful per-shop listing indexes -------------------------------------
CREATE INDEX idx_invoices_shop_date ON invoices (shop_id, invoice_date);
CREATE INDEX idx_purchases_shop_date ON purchases (shop_id, purchase_date);
CREATE INDEX idx_quotations_shop_date ON quotations (shop_id, quotation_date);
