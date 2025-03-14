ALTER TABLE klanke
    ADD COLUMN sak_type TEXT,
    ADD COLUMN sak_fagsaksystem TEXT;

ALTER TABLE klanke
    RENAME COLUMN internal_saksnummer TO sak_fagsakid;