create table anke
(
    id                  serial primary key,
    foedselsnummer      varchar(11),
    fritekst            varchar,
    status              varchar(15) not null default 'DRAFT',
    tema                varchar(3)  not null default 'UKJ',
    modified_by_user    timestamptz not null default now(),
    created             timestamptz not null default now(),
    journalpost_id      varchar(50),
    vedtak_date         date,
    internal_saksnummer uuid UNIQUE,
    fullmektig          varchar(11),
    language            text
);

create table anke_vedlegg
(
    id                       serial primary key,
    anke_id                  integer      not null references anke (id),
    tittel                   varchar(250),
    ref                      varchar(500) null,
    content_type             varchar(50)  not null default 'Ukjent',
    size_in_bytes            integer      not null default 0,
    anke_internal_saksnummer uuid         not null
);