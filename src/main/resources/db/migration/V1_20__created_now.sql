ALTER TABLE anke
    alter column created set default now();

ALTER TABLE anke
    alter column modified_by_user set default now();