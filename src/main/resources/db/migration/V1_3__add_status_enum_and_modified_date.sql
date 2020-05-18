CREATE TYPE KlageStatusType AS ENUM ('DRAFT', 'DONE', 'DELETED');

ALTER TABLE klage
    ADD COLUMN status KlageStatusType NOT NULL DEFAULT 'DRAFT';

ALTER TABLE klage
    ADD COLUMN modifiedByUser timestamptz NOT NULL DEFAULT now();
