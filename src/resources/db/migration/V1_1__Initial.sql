create sequence klageId_seq start with 1 increment by 1;

create table klage (
    id int not null,
    klageId int not null,
    foedselsnummer varchar(11),
    fritekst varchar,
    constraint klage_pk primary key (id)
);
