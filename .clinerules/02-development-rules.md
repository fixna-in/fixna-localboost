# Development Rules

Use clean, modular, testable code. Follow SOLID where useful.
Controllers only translate HTTP to application calls.
Business logic belongs in services/domain components.
Avoid duplication.
Do not modify unrelated files.
Do not remove behavior without approval.
All externally visible API changes require OpenAPI/documentation updates.
All new features require validation, error handling and tests.
