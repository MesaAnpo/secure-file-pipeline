# Contributing

Thank you for considering a contribution! Please open an issue to discuss new features or bugs before submitting code.

## Development Setup

1. Fork the repository and create a branch for your change.
2. Run `docker-compose up` from the `infrastructure` directory.
3. Execute `flake8 scanner-worker/scanner` and `pytest -q` for the worker tests.
4. Run `mvn -f api-gateway/pom.xml clean verify` for the API gateway.

## Style Guide

- Python code is formatted with `flake8` rules.
- Java code follows the provided `checkstyle.xml` configuration.

## Pull Requests

1. Ensure all tests pass and new tests are added where needed.
2. Update `CHANGELOG.md` with a short description under the `Unreleased` section.
3. Submit your merge request against the `main` branch.

We appreciate every contribution!
