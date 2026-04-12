# Query Analyzer

A Spring Boot application that analyzes SQL queries and provides execution insights using PostgreSQL's `EXPLAIN ANALYZE`.

## Features

- Execute SQL queries and retrieve detailed execution plans (JSON format)
- Analyze query performance with `EXPLAIN (ANALYZE, BUFFERS)`
- Detect performance issues: sequential scans, nested loops, planner estimate mismatches, low cache hit ratios, disk sort spills
- Actionable optimization suggestions
- SQL injection protection with keyword blocklist + read-only transactions
- REST API with Swagger UI for interactive testing
- Health monitoring via Spring Actuator

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.2
- **Database:** PostgreSQL
- **Build Tool:** Maven
- **API Docs:** Swagger / OpenAPI (springdoc)
- **Monitoring:** Spring Actuator

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL running locally

### 1. Clone the repository

```bash
git clone https://github.com/irfanzameel/sql-query-analyzer.git
cd sql-query-analyzer
```

### 2. Set up the database

```bash
# Create the database
psql -U postgres -c "CREATE DATABASE querydb;"

# (Recommended) Create a read-only user for safety
psql -U postgres -d querydb -c "
  CREATE USER analyzer_readonly WITH PASSWORD 'readonly_pass';
  GRANT CONNECT ON DATABASE querydb TO analyzer_readonly;
  GRANT USAGE ON SCHEMA public TO analyzer_readonly;
  GRANT SELECT ON ALL TABLES IN SCHEMA public TO analyzer_readonly;
  ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO analyzer_readonly;
"
```

### 3. Configure environment variables

```bash
cp .env.example .env
# Edit .env with your database credentials
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
```

Or use the defaults (username: `postgres`, password: `postgres`).

### 4. Run the application

```bash
./mvnw spring-boot:run
```

### 5. Try it out

**Swagger UI:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

**Health Check:** [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

**Example curl:**

```bash
curl -X POST http://localhost:8080/analyze \
  -H "Content-Type: application/json" \
  -d '{"sql": "SELECT * FROM users WHERE email = '\''test@example.com'\''", "verbose": true}'
```

**Example response:**

```json
{
  "success": true,
  "data": {
    "planningTimeMs": 0.123,
    "executionTimeMs": 12.456,
    "slowQuery": false,
    "scanTypes": ["Seq Scan"],
    "issues": ["Sequential scan on large table (10000 estimated rows)"],
    "suggestions": ["Consider adding an index on filtered columns"],
    "rawPlan": "..."
  },
  "error": null,
  "timestamp": "2026-04-12T08:55:00Z"
}
```

### Docker (optional)

```bash
docker compose up -d   # starts PostgreSQL on port 5432
./mvnw spring-boot:run
```

## API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/analyze` | Analyze a SQL query |
| `GET` | `/actuator/health` | Health check |
| `GET` | `/swagger-ui/index.html` | Interactive API docs |

### POST `/analyze`

**Request body:**

```json
{
  "sql": "SELECT * FROM users WHERE id = 1",
  "verbose": false
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `sql` | string | yes | The SELECT query to analyze |
| `verbose` | boolean | no | Include raw EXPLAIN plan in response (default: false) |

## Running Tests

```bash
./mvnw test
```

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file.

## Contact

**Author:** Irfan Zameel K A  
**Email:** irfanzameelka@gmail.com  
**GitHub:** [irfanzameel](https://github.com/irfanzameel)  
**LinkedIn:** [Irfan Zameel](https://www.linkedin.com/in/irfan-zameel-ka-52162b288)
