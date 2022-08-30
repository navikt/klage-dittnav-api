CREATE TABLE anke
(
    id                  UUID,
    foedselsnummer      TEXT,
    fritekst            TEXT,
    status              TEXT,
    tema                TEXT,
    user_saksnummer     TEXT,
    modified_by_user    TIMESTAMP WITH TIME ZONE NOT NULL,
    created             TIMESTAMP WITH TIME ZONE NOT NULL,
    vedtak_date         DATE,
    enhetsnummer        TEXT,
    language            TEXT,
    title_key           TEXT,
    has_vedlegg         BOOLEAN DEFAULT FALSE
);