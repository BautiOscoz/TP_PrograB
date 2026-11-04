# Database installation

1. In pgAdmin, connect to the local PostgreSQL server as `postgres`.
2. Create a database named `Championship` (capital C). With SQL, use `CREATE DATABASE "Championship";` separately, outside a transaction.
3. Open Query Tool **inside Championship** and run the entire `database/setup.sql` file.
4. Set `CHAMPIONSHIP_DB_PASSWORD` in the Java run configuration environment to the password of your PostgreSQL `postgres` account.
5. Run `GUI.AppLauncher`. The configured server is `localhost:5432`.

The setup creates `public.pais`, `public.ciudad`, and `public.estadio`, with generated BIGINT IDs, mandatory names and foreign keys. Cities with stadiums cannot be deleted. It loads the countries from the current tournament JSON and 26 cities/stadiums, enough for the 13 knockout fixtures.

The script uses one transaction and can be repeated without duplicating seed records. It does not delete data. It targets a fresh database or a compatible schema; it does not repair older schemas. Existing duplicate names can prevent unique-index creation and roll back the setup. After a reported error, execute `ROLLBACK;` before retrying.

`add_venues.sql` only adds venues to an already configured database. It is not required after `setup.sql`.

Verification:
```sql
SELECT COUNT(*) FROM pais;
SELECT COUNT(*) FROM ciudad;
SELECT COUNT(*) FROM estadio;
SELECT e.nombre AS estadio, c.nombre AS ciudad, p.nombre AS pais
FROM estadio e JOIN ciudad c ON c.id = e.ciudad
JOIN pais p ON p.id = c.id_pais;
```
