-- V15: each shop can upload a logo that is printed on invoices, quotations, credit notes and enquiries
ALTER TABLE branding ADD COLUMN logo_data bytea;
ALTER TABLE branding ADD COLUMN logo_type varchar(50);