ALTER TABLE klage
        ADD COLUMN tema varchar(3) NOT NULL DEFAULT 'UKJ';

ALTER TABLE klage
        ADD COLUMN enhet_id varchar(4) NULL;
