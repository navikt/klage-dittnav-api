ALTER TABLE klage
    ADD COLUMN has_vedlegg BOOLEAN default FALSE;

ALTER TABLE anke
    ADD COLUMN has_vedlegg BOOLEAN default FALSE;
