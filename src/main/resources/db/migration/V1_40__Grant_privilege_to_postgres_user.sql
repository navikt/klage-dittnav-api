DO
$$
    BEGIN
        IF EXISTS
                (SELECT 1 from pg_roles where rolname = 'postgres')
        THEN
            GRANT USAGE ON SCHEMA public TO postgres;
            GRANT SELECT ON ALL TABLES IN SCHEMA public TO postgres;
            ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO postgres;
        END IF;
    END
$$;
