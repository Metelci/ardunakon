# Architecture Decision Record: KDoc Documentation Standards

**ADR**: 004  
**Title**: KDoc Standards for Public APIs  
**Date**: December 29, 2025  
**Status**: Accepted  
**Deciders**: Engineering Team

## Context

The codebase has grown to include multiple subsystems (control, connectivity, security, telemetry),
and public APIs are shared across ViewModels, managers, and protocol utilities. Documentation
coverage has been inconsistent, which slows onboarding and increases the risk of incorrect usage.

## Decision

Adopt a consistent KDoc standard for all public classes, methods, and properties. Documentation
must include a brief summary, details for complex behavior, and structured tags:
- `@param` for parameters
- `@return` for non-Unit returns
- `@throws` for exceptions

## Rationale

- Improves developer onboarding with quick, in-editor guidance.
- Reduces misuse of API contracts and ambiguous behavior.
- Establishes a shared vocabulary for complex systems (protocol, security, networking).
- Aligns with Kotlin tooling and supports generated documentation.

## Implementation

- Apply KDoc to high-priority components first (security, protocol, connectivity).
- Expand coverage to ViewModels and data models.
- Keep a lightweight documentation plan in `plans/kdoc_documentation_plan.md`.
- Review KDoc updates during code review for correctness and clarity.

## Consequences

### Positive
- Clearer API contracts and fewer usage errors.
- Faster onboarding for new contributors.
- Improved long-term maintainability and supportability.

### Negative
- Additional effort required to keep documentation current.
- Risk of stale KDoc if not maintained during refactors.

### Neutral
- No runtime impact; changes are documentation only.

## Review Date
2026-06-29

## References
- [KDoc Documentation Plan](../../plans/kdoc_documentation_plan.md)
- [Architecture Overview](../../ARCHITECTURE.md)
