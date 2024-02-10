--use anke as base for common class, since it already has id as uuid.
ALTER TABLE anke
    RENAME TO klanke;

--add enhetsnummer from anke to make merging them later easier.
--Also add klanke_type for types in JPA.
--Add "new id as uuid" so we can keep the old id while migrating tables/relations.
ALTER TABLE klage
    ADD COLUMN enhetsnummer TEXT,
    ADD COLUMN klanke_type  TEXT,
    ADD COLUMN new_uuid_id  uuid DEFAULT gen_random_uuid();

--set correct type for klager before merging into common table
UPDATE klage
SET klanke_type = 'klage';

--make sure common table has klage prop also.
--Also type for JPA.
ALTER TABLE klanke
    ADD COLUMN checkboxes_selected TEXT,
    ADD COLUMN klanke_type         TEXT;

--correct type for anker in the common table
UPDATE klanke
SET klanke_type = 'anke';

--change id of vedlegg tables to uuid. Should not matter.
ALTER TABLE klage_vedlegg
    ALTER id DROP DEFAULT,
    ALTER id TYPE uuid USING (gen_random_uuid());

ALTER TABLE anke_vedlegg
    ALTER id DROP DEFAULT,
    ALTER id TYPE uuid USING (gen_random_uuid());

--adding new fk prop in klage_vedlegg for the new uuid fks.
ALTER TABLE klage_vedlegg
    ADD COLUMN new_klage_id uuid;

--populate all new fks
UPDATE klage_vedlegg
SET new_klage_id = (SELECT new_uuid_id FROM klage WHERE id = klage_id);

--drop old fk constraint. Might not need to since we will delete the column.
ALTER TABLE klage_vedlegg
    DROP CONSTRAINT vedlegg_klage_id_fkey;

--drop old fk
ALTER TABLE klage_vedlegg
    DROP COLUMN klage_id;

--make the new uuid prop, preparing to be fk later.
ALTER TABLE klage_vedlegg
    RENAME COLUMN new_klage_id TO klage_id;

--right now there is now fk relation to klage_vedlegg, so can drop old serial id.
ALTER TABLE klage
    DROP COLUMN id;

--migrate to new id as uuid.
ALTER TABLE klage
    RENAME COLUMN new_uuid_id TO id;

--make it primary
ALTER TABLE klage
    ADD PRIMARY KEY (id);

--turn on fk to klage_vedlegg which should now work
ALTER TABLE klage_vedlegg
    ADD CONSTRAINT fk_klage_vedlegg
        FOREIGN KEY (klage_id)
            REFERENCES klage (id);

--use anke_vedlegg as template for common vedlegg table
ALTER TABLE anke_vedlegg
    RENAME TO vedlegg;

--drop old fk before we make a common table
ALTER TABLE vedlegg
    DROP CONSTRAINT anke_vedlegg_anke_id_fkey1;

--rename fk to match klage/anke/ettersendelse etc.
ALTER TABLE vedlegg
    RENAME COLUMN anke_id TO klanke_id;

--make klage_vedlegg similar to vedlegg to ease migration.
ALTER TABLE klage_vedlegg
    RENAME COLUMN klage_id TO klanke_id;

--insert all klage_vedlegg into shared table.
INSERT INTO vedlegg
SELECT *
FROM klage_vedlegg;

--TODO indexes