create table vedlegg(
    id      serial primary key,
    klageId integer not null references klage(id),
    tittel  varchar(250),
    gcsRef  varchar(500) null
);
