UPDATE klage
SET innsendingsytelse = tema
WHERE innsendingsytelse IS NULL;

UPDATE anke
SET innsendingsytelse = tema
WHERE innsendingsytelse IS NULL;

ALTER TABLE anke
    ALTER COLUMN innsendingsytelse SET NOT NULL;

ALTER TABLE klage
    ALTER COLUMN innsendingsytelse SET NOT NULL;