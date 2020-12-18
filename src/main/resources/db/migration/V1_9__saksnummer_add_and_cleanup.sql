ALTER TABLE klage
    RENAME COLUMN saksnummer TO user_saksnummer;

ALTER TABLE klage
    ADD COLUMN internal_saksnummer text;

