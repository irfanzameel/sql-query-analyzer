# Contributing to Query Analyzer

Thanks for your interest in contributing! Here's how to get started.

## Development Setup

1. **Fork & clone** the repository
2. **Start PostgreSQL** — use Docker or install locally:
   ```bash
   docker compose up -d
   ```
3. **Set environment variables:**
   ```bash
   cp .env.example .env
   # Edit .env with your credentials
   ```
4. **Run the app:**
   ```bash
   ./mvnw spring-boot:run
   ```
5. **Run tests:**
   ```bash
   ./mvnw test
   ```

## Guidelines

- **Write tests** for new features and bug fixes
- **Keep commits focused** — one logical change per commit
- **Follow existing code style** (Java conventions, consistent formatting)
- **Only SELECT queries** are allowed through the analyzer — do not weaken SQL validation

## Submitting Changes

1. Create a feature branch: `git checkout -b feature/your-feature`
2. Make your changes and add tests
3. Ensure all tests pass: `./mvnw test`
4. Push and open a Pull Request against `main`

## Reporting Bugs

Use the [Bug Report](https://github.com/irfanzameel/sql-query-analyzer/issues/new?template=bug_report.md) issue template.
