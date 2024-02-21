UPDATE klanke
SET klanke_type = 'ANKE'
WHERE klanke_type = 'anke';

UPDATE klanke
SET klanke_type = 'KLAGE'
WHERE klanke_type = 'klage';

ALTER TABLE klanke
    ALTER COLUMN klanke_type SET NOT NULL;
