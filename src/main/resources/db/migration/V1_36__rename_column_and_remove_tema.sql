ALTER TABLE klanke
    RENAME COLUMN innsendingsytelse TO innsendingsytelse_id;

ALTER TABLE klanke
    DROP COLUMN tema;