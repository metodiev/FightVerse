-- The initial schema declared token_hash as CHAR(64) while RefreshToken maps it as a
-- 64-character String, so Hibernate's ddl-auto=validate rejected the schema:
--
--   Schema validation: wrong column type encountered in column [token_hash] in table
--   [refresh_tokens]; found [bpchar (Types#CHAR)], but expecting [varchar(64) (Types#VARCHAR)]
--
-- The application could not start against a real database because of it. VARCHAR also avoids
-- CHAR's blank-padding semantics for a value that is always exactly 64 hex characters.
--
-- Added as a new migration rather than editing V2 so databases that already applied V1-V6 keep
-- valid Flyway checksums.

ALTER TABLE refresh_tokens ALTER COLUMN token_hash TYPE VARCHAR(64);
