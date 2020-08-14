ALTER TABLE klage RENAME COLUMN referanse TO saksnummer;
ALTER TABLE klage RENAME COLUMN vedtaksdato TO vedtak;
ALTER TABLE klage DROP COLUMN enhet_id;
ALTER TABLE klage DROP COLUMN ytelse;
ALTER TABLE klage DROP COLUMN journalpost_status;
