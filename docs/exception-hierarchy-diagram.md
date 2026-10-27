# Exception Hierarchy Diagram — EcoPoint Edu

This document details the custom exception hierarchy implemented in the `com.ecopoint.exceptions` package.

## 1. Exception Inheritance Hierarchy

```mermaid
classDiagram
    direction TB

    class RuntimeException {
        <<java.lang>>
    }

    class EcoPointException {
        <<abstract>>
        -String errorCode
        #EcoPointException(String message)
        #EcoPointException(String message, String errorCode)
        #EcoPointException(String message, String errorCode, Throwable cause)
        +getErrorCode() String
    }

    class InvalidDataException {
        +String ERROR_CODE = "ERR_INVALID_DATA"$
        -String fieldName
        +InvalidDataException(String fieldName, String problemDescription)
        +InvalidDataException(String message)
        +getFieldName() String
    }

    class BusinessRuleException {
        +String ERROR_CODE = "ERR_BUSINESS_RULE"$
        +BusinessRuleException(String message)
        +BusinessRuleException(String message, String errorCode)
    }

    class ResourceNotFoundException {
        +String ERROR_CODE = "ERR_NOT_FOUND"$
        -String resourceType
        -Object resourceId
        +ResourceNotFoundException(String resourceType, Object resourceId)
        +getResourceType() String
        +getResourceId() Object
    }

    class PersistenceException {
        +String ERROR_CODE = "ERR_PERSISTENCE"$
        +PersistenceException(String operationContext, Throwable cause)
        +PersistenceException(String message)
    }

    RuntimeException <|-- EcoPointException
    EcoPointException <|-- InvalidDataException
    EcoPointException <|-- BusinessRuleException
    EcoPointException <|-- ResourceNotFoundException
    EcoPointException <|-- PersistenceException
```

---

## 2. Exception Classification and Trigger Matrix

| Exception Class | Error Code | Primary Trigger Scenarios in EcoPoint Edu |
| :--- | :---: | :--- |
| **`InvalidDataException`** | `ERR_INVALID_DATA` | • Required fields are `null` or blank.<br>• User `firstName`, `lastName`, or `role` contains numbers or special symbols.<br>• User `email` is missing the `@` symbol or has an invalid domain.<br>• Map coordinates (`coordX`, `coordY`) are outside `[0.00, 100.00]`.<br>• Container capacity or quartering weights are $\le 0$.<br>• Inspection fill level is not in `{VACIO, BAJO, MEDIO, ALTO, DESBORDADO}`.<br>• Evidence file path does not end in `.jpg`, `.jpeg`, `.png`, or `.webp`. |
| **`BusinessRuleException`** | `ERR_BUSINESS_RULE` | • Attempting to register a `User` with an email address that already exists.<br>• Attempting to register a duplicate `Zone` name inside the same `Institution`.<br>• Registering an `EcologicalPoint` without the exact 3 containers required by Resolution 2184 of 2019 (`BLANCO`, `VERDE`, `NEGRO`).<br>• Submitting a `Quartering` where `sampleWeight > initialWeight`.<br>• Submitting a `Quartering` where the sum of classified weights exceeds `sampleWeight`.<br>• Attempting to register more than one `Quartering` for the same `InspectionRecord`.<br>• Attempting to mark an already attended `Alert` as attended again. |
| **`ResourceNotFoundException`** | `ERR_NOT_FOUND` | • Searching for a `User`, `Institution`, `Zone`, `EcologicalPoint`, `InspectionDay`, `InspectionRecord`, `Quartering`, or `Alert` by an ID or email that does not exist. |
| **`PersistenceException`** | `ERR_PERSISTENCE` | • Failures occurring during data storage or retrieval operations. |
