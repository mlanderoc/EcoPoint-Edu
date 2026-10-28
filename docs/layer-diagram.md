# Layered Architecture Diagram — EcoPoint Edu

This document describes the layered architecture of **EcoPoint Edu**, including the Java Swing Presentation Layer (`com.ecopoint.ui`), Business Service Layer (`com.ecopoint.service`), Validation Layer (`com.ecopoint.validation`), Protected Variations Persistence Layer (`com.ecopoint.persistence`), Domain Model Layer (`com.ecopoint.model`), and Cross-Cutting Exception Layer (`com.ecopoint.exceptions`).

## 1. Package Dependency and Layer Diagram

```mermaid
flowchart TD
    subgraph UI_LAYER["Presentation Layer — Java Swing (com.ecopoint.ui)"]
        LF["LoginFrame"]
        MF["MainFrame"]
        DAP["DashboardAndAlertsPanel"]
        CPP["CampusAndPointPanel (Interactive % Map)"]
        IMP["InspectionManagementPanel"]
        QMP["QuarteringManagementPanel"]
        UMP["UserManagementPanel"]
        LF --> MF
        MF --> DAP
        MF --> CPP
        MF --> IMP
        MF --> QMP
        MF --> UMP
    end

    subgraph SERVICE_LAYER["Business Logic Layer (com.ecopoint.service)"]
        US["UserService"]
        EPS["EcologicalPointService"]
        IS["InspectionService"]
        AS["AlertService"]
        QS["QuarteringService"]
        IS --> EPS
        IS --> US
        IS --> AS
        QS --> IS
    end

    subgraph VALIDATION_LAYER["Validation Layer (com.ecopoint.validation)"]
        UV["UserValidator"]
        EPV["EcologicalPointValidator"]
        IV["InspectionValidator"]
        QV["QuarteringValidator"]
    end

    subgraph PERSISTENCE_LAYER["Persistence Layer — Protected Variations (com.ecopoint.persistence)"]
        REPO_INT["Repository Interfaces (Stable Contract)\nUserRepository · InstitutionRepository · ZoneRepository\nEcologicalPointRepository · InspectionRepository\nQuarteringRepository · AlertRepository"]
        FILE_IMPL["Flat-File Implementations (data/*.txt)\nFileUserRepository · FileInstitutionRepository · FileZoneRepository\nFileEcologicalPointRepository · FileInspectionRepository\nFileQuarteringRepository · FileAlertRepository"]
        FILE_IMPL -. "implements" .-> REPO_INT
    end

    subgraph MODEL_LAYER["Domain Model Layer (com.ecopoint.model)"]
        M_ALL["Institution · Zone · EcologicalPoint · Container · User\nInspectionDay · InspectionRecord · ContainerDetail\nAlert · Evidence · WasteCategory · Quartering · QuarteringResult"]
    end

    subgraph EXCEPTION_LAYER["Cross-Cutting Exception Layer (com.ecopoint.exceptions)"]
        EPE["EcoPointException (Abstract Root)\n├── InvalidDataException\n├── BusinessRuleException\n├── ResourceNotFoundException\n└── PersistenceException"]
    end

    UI_LAYER --> SERVICE_LAYER
    SERVICE_LAYER --> VALIDATION_LAYER
    SERVICE_LAYER --> REPO_INT
    SERVICE_LAYER --> MODEL_LAYER
    VALIDATION_LAYER --> MODEL_LAYER
    PERSISTENCE_LAYER --> MODEL_LAYER

    VALIDATION_LAYER -. "Throws InvalidData / BusinessRule / NotFound" .-> EXCEPTION_LAYER
    PERSISTENCE_LAYER -. "Throws PersistenceException" .-> EXCEPTION_LAYER
    UI_LAYER -. "Catches EcoPointException & displays Spanish dialogs" .-> EXCEPTION_LAYER
```

---

## 2. Layer Responsibilities and Protected Variations

| Layer | Package | Core Responsibility |
| :--- | :--- | :--- |
| **Presentation Layer** | `com.ecopoint.ui` | Java Swing graphical interface applying the official EcoPoint Edu logo color palette. Provides session login, full CRUD forms, interactive percentage map, quartering calculator, and dashboard statistics. |
| **Business Service Layer** | `com.ecopoint.service` | Orchestrates domain use cases, delegates precondition checks to validators, triggers automatic alerts, computes consolidated statistics, and persists changes through `*Repository` interfaces. |
| **Validation Layer** | `com.ecopoint.validation` | Stateless precondition gates verifying syntactic format and domain rules (Resolution 2184 of 2019 3-color rule, unique user emails, quartering weight coherence). |
| **Persistence Layer** | `com.ecopoint.persistence` | Applies **Protected Variations** and **Dependency Inversion (DIP)** via `*Repository` interfaces. Currently implemented over UTF-8 flat files (`data/*.txt`) and ready for seamless database migration. |
| **Domain Model Layer** | `com.ecopoint.model` | Encapsulates the 13 domain entities and their Information Expert calculations. |
| **Cross-Cutting Exceptions** | `com.ecopoint.exceptions` | Unchecked exception hierarchy rooted in `EcoPointException` carrying user-facing Spanish error messages. |
