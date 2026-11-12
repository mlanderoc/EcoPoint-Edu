# Layered Architecture Diagram — EcoPoint Edu

This document describes the target layered architecture of **EcoPoint Edu** after the restructuring, and which layers already exist. The full rules are in [`AGENTS.md`](../AGENTS.md).

## 1. Layers and dependency direction

```mermaid
flowchart TD
    FE["Web frontend — React + Vite, MapLibre GL JS<br/>(specs 004–007, pending)"]
    API["controller — REST API /api/v1, Spring Boot<br/>(spec 003, pending)"]
    SVC["service — use cases, transactions<br/>(specs 002–006, pending)"]
    VAL["validation — stateless validators + AccessPolicy<br/>(spec 001, done)"]
    MOD["model — characterization domain<br/>(spec 001, done)"]
    PER["persistence — repository interfaces + PostgreSQL implementations<br/>(spec 002, pending)"]
    DB[("PostgreSQL 16+")]
    EXC["exceptions — EcoPointException hierarchy<br/>(cross-cutting, done)"]

    FE -->|HTTP + JSON| API
    API --> SVC
    SVC --> VAL
    SVC --> PER
    VAL --> MOD
    PER --> MOD
    PER --> DB
    SVC --> MOD

    EXC -.-> API
    EXC -.-> SVC
    EXC -.-> VAL
    EXC -.-> MOD
    EXC -.-> PER
```

## 2. Dependency rules

| Package | May import | Must not import |
| --- | --- | --- |
| `model` | `exceptions` | `validation`, `service`, `persistence`, `controller` |
| `validation` | `model`, `exceptions` | `service`, `persistence`, `controller` |
| `persistence` | `model`, `exceptions` | `service`, `controller` |
| `service` | `validation`, `model`, `persistence` (interfaces only), `exceptions` | `controller`, persistence implementations |
| `controller` | `service`, DTOs, `exceptions` | `persistence`, business rules |

## 3. Why this structure

* **Low Coupling and Protected Variations:** services depend on repository interfaces, so changing the database does not touch the business rules.
* **Pure Fabrication:** validators hold the rules that do not belong to a single entity and need data from several objects, received as parameters.
* **Information Expert:** calculations (net weight, density, composition, 2 % control) live in the domain classes that own the data.
* **Testability:** the domain and the validators are plain Java and are tested without a database, a server or a browser.
