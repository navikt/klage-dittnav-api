create table vedlegg(
    id      serial,
    klageId int not null,
    tittel  varchar(250),
    gcsRef  varchar(500) null
)
