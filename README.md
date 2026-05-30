# mud-stock

Minimal Spring Boot application that connects to a local MySQL database.

Configuration is in `src/main/resources/application.yml`.

Run:

```bash
mvn spring-boot:run
```

The app exposes a small REST API at `GET /api/stocks` and `POST /api/stocks`.
