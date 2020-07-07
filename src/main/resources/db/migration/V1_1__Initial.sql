create table klage
(
    id               serial primary key,
    foedselsnummer   varchar(11),
    fritekst         varchar,
    status           varchar(15) not null default 'DRAFT',
    tema             varchar(3)  not null default 'UKJ',
    enhet_id         varchar(4)  null,
    vedtaksdato      varchar(100) not null,
    referanse        varchar(25) null,
    modified_by_user timestamptz not null default now(),
    created          timestamptz not null default now()
);

create table vedlegg
(
    id              serial primary key,
    klage_id        integer         not null references klage (id),
    tittel          varchar(250),
    ref             varchar(500)    null,
    content_type    varchar(50)     not null default 'Ukjent',
    size_in_bytes   integer         not null default 0
);

