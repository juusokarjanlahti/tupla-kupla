# tupla-kupla

## Local development

### Database

Postgres runs locally in Docker. From the repository root:

```bash
docker compose up -d
```

That starts Postgres 18 on `localhost:5432` with database, user and password all set to
`tuplakupla`. Data persists in the `tupla-kupla_db-data` volume between restarts.

| Command | What it does |
| --- | --- |
| `docker compose up -d` | Start Postgres in the background |
| `docker compose ps` | Show status and health |
| `docker compose logs -f db` | Tail the database log |
| `docker compose stop` | Stop the container, keep the data |
| `docker compose down -v` | Stop and **delete all data** |

To open a psql shell:

```bash
docker compose exec db psql -U tuplakupla -d tuplakupla
```

If port 5432 is already taken, or you want different credentials, copy `.env.example` to
`.env` and edit it. `.env` is gitignored; without it the defaults above apply.

Note that `.env` is read by Docker Compose only. The backend reads the same variable names
from the actual environment, so if you change `.env` you also need to export those values
(or set them in your IDE's run configuration) before starting the backend.

### Backend

The backend defaults to the credentials above, so with the database running and `.env`
untouched no extra configuration is needed:

```bash
cd backend && ./mvnw spring-boot:run
```

The database must be up before starting the backend or running `./mvnw test`.
