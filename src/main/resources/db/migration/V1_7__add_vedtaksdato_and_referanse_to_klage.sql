alter table klage
        add column vedtaksdato timestamp not null;

alter table klage
        add column referanse varchar(25) null;
