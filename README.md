# mud-stock

Minimal Spring Boot application that connects to a local MySQL database.

Configuration is in `src/main/resources/application.yml`.

Run:

```bash
export SPRING_PROFILES_ACTIVE=server
mvn spring-boot:run 
mvn spring-boot:run -Dspring-boot.run.profiles=server
```

The app exposes a small REST API at `GET /api/stocks` and `POST /api/stocks`.
