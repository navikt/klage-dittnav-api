ALTER TABLE klage
    ADD COLUMN vedtak_type VARCHAR(25);
ALTER TABLE klage
    ADD COLUMN vedtak_date date;
ALTER TABLE klage
    ALTER COLUMN vedtak DROP NOT NULL;
