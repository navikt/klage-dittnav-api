CREATE USER "klage-user" WITH PASSWORD 'klage-user';
CREATE USER "klage-admin" WITH PASSWORD 'klage-admin';
ALTER DEFAULT PRIVILEGES FOR ROLE "klage-admin" IN SCHEMA public GRANT ALL ON TABLES TO "klage-user";
ALTER DEFAULT PRIVILEGES FOR ROLE "klage-admin" IN SCHEMA public GRANT ALL ON SEQUENCES TO "klage-user";