-- V18: height of the shop logo on A4 prints (mm). Adds the column V16 was meant to add.
ALTER TABLE branding ADD COLUMN IF NOT EXISTS logo_height_mm INTEGER NOT NULL DEFAULT 18;
ALTER TABLE branding DROP CONSTRAINT IF EXISTS branding_logo_height_mm_check;
ALTER TABLE branding ADD CONSTRAINT branding_logo_height_mm_check CHECK (logo_height_mm BETWEEN 8 AND 50);