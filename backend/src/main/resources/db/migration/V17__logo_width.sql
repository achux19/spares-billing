-- V17: widest the shop's logo may print on A4 documents (millimetres). The picture keeps its proportions.
ALTER TABLE branding ADD COLUMN logo_width_mm INTEGER NOT NULL DEFAULT 60
    CHECK (logo_width_mm BETWEEN 15 AND 120);
