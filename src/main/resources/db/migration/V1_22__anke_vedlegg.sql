-- Add primary key to anke
ALTER TABLE anke
    ALTER COLUMN id SET NOT NULL;

ALTER TABLE anke
    ADD PRIMARY KEY (id);

-- Add vedlegg to anke
create table anke_vedlegg
(
    id            serial primary key,
    anke_id       uuid         not null references anke (id),
    tittel        varchar(250),
    ref           varchar(500) null,
    content_type  varchar(50)  not null default 'Ukjent',
    size_in_bytes integer      not null default 0
);

-- Distinguish between klage and anke
ALTER TABLE vedlegg
    RENAME TO klage_vedlegg;