DO
$$
    BEGIN
        IF EXISTS
                (SELECT 1 from pg_roles where rolname = 'postgres')
        THEN
            GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO postgres;
            ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON SEQUENCES TO postgres;
        END IF;
    END
$$;
