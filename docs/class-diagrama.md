# Class Diagram — EcoPoint Edu Domain (spec 001)

This document presents the UML class diagram of the **solid waste characterization domain** of EcoPoint Edu, built in spec 001: the domain model (`com.ecopoint.model`), the stateless validators (`com.ecopoint.validation`) and the exception hierarchy (`com.ecopoint.exceptions`).

Persistence (spec 002), REST API (spec 003) and web screens (specs 004 to 007) are not part of this diagram yet.

> The previous model (ecological points, Swing UI and flat files) is kept in the git tag `v1-modelo-anterior`.

## 1. Domain model

```mermaid
classDiagram
    direction LR

    class Institution {
        -Long id
        -String name
        -String address
        -GeoPoint mapCenter
        -int mapZoom
        -CharacterizationSettings settings
        +hasMapCenter() boolean
    }
    class GenerationSource {
        -Long id
        -Long institutionId
        -String code
        -String name
        -SourceType type
        -GeoPolygon geometry
        -boolean active
        +isValidCode(String)$ boolean
        +hasGeometry() boolean
        +isAvailableForRegistration() boolean
        +deactivate()
    }
    class CharacterizationStudy {
        -Long id
        -Long institutionId
        -StudyStatus status
        -List~SamplingDay~ days
        -List~StudyReopening~ reopenings
        +scheduleDates(List~LocalDate~)
        +isDayExcluded(int) boolean
        +getIncludedDayNumbers() List~Integer~
        +getStartDate() LocalDate
        +getEndDate() LocalDate
        +overlaps(CharacterizationStudy) boolean
        +changeStatus(StudyStatus)
        +reopen(Long, String, LocalDateTime)
    }
    class SamplingDay {
        -int dayNumber
        -LocalDate date
        +isExcluded() boolean
        +isScheduled() boolean
    }
    class SamplingShift {
        -Long id
        -Long studyId
        -int dayNumber
        -ShiftType type
        -ShiftStatus status
        -ShiftPopulation population
        -List~WasteBag~ bags
        -Quartering quartering
        -DensityMeasurement density
        -Set~Long~ sourcesWithoutGeneration
        +getCode() String
        +registerPopulation(ShiftPopulation)
        +addBag(WasteBag) BagLabel
        +markSourceWithoutGeneration(Long)
        +isSourceCovered(Long) boolean
        +calculateTotalNetWeight() BigDecimal
        +registerQuartering(Quartering)
        +registerDensity(DensityMeasurement)
        +close()
        +reopen()
    }
    class VerifiableRecord {
        <<abstract>>
        -Long id
        -VerificationInfo verification
        +getRecordType()* String
        +isPendingVerification() boolean
    }
    class VerificationInfo {
        -VerificationStatus status
        -Long enteredBy
        -LocalDateTime enteredAt
        -Long verifiedBy
        -LocalDateTime verifiedAt
        +markVerified(Long, LocalDateTime)
        +markCorrected(Long, LocalDateTime)
        +wasEnteredBy(Long) boolean
    }
    class ShiftPopulation {
        -int students
        -int teachers
        -int staff
        +getTotal() int
    }
    class WasteBag {
        -Long sourceId
        -String sourceCode
        -BagLabel label
        -BigDecimal grossWeight
        -BigDecimal tare
        -BigDecimal volume
        -boolean atypical
        +getNetWeight() BigDecimal
        +getDensity() BigDecimal
        +getLabelText() String
    }
    class Quartering {
        -Long shiftId
        -BigDecimal shiftNetWeight
        -BigDecimal sampleWeight
        -List~QuarteringResult~ results
        -QualityStatus qualityStatus
        +isFullSample() boolean
        +calculateTotalClassifiedWeight() BigDecimal
        +calculateDifferencePercent() BigDecimal
        +evaluateQuality(BigDecimal) QualityStatus
        +calculateCompositionPercentages()
        +calculateGroupComposition() Map
        +hasHazardousResults() boolean
        +annul(String, Long, LocalDateTime)
    }
    class QuarteringResult {
        -Long categoryId
        -String categoryName
        -WasteGroup group
        -BigDecimal weight
        -BigDecimal percentage
        +calculatePercentageOver(BigDecimal) BigDecimal
    }
    class DensityMeasurement {
        -Long shiftId
        -BigDecimal containerVolume
        -BigDecimal netWeight
        +getDensity() BigDecimal
    }
    class WasteCategory {
        -Long id
        -String name
        -WasteGroup group
        -boolean active
        +normalizeName(String)$ String
        +isHazardous() boolean
        +isRecoverable() boolean
    }
    class User {
        -Long id
        -String firstName
        -String lastName
        -String email
        -UserRole role
        -Long institutionId
        -boolean active
        +getFullName() String
        +hasPermission(Permission) boolean
    }
    class Crew {
        -Long id
        -String name
        -User responsible
        -List~User~ members
        +hasResponsibleTeacher() boolean
        +addMember(User) boolean
    }
    class Alert {
        -AlertType type
        -String targetType
        -Long targetId
        -String description
        -boolean attended
        +attend(User, String, LocalDateTime)
    }
    class AlertFactory {
        <<utility>>
        +forNonConformingQuartering(Quartering, String, LocalDateTime)$ Alert
        +forAtypicalBag(WasteBag, LocalDateTime)$ Alert
        +forPendingVerification(SamplingShift, int, LocalDateTime)$ Alert
    }
    class Correction {
        <<record>>
        recordType, recordId, field
        oldValue, newValue, reason
        userId, at
    }
    class Evidence {
        -EvidenceTarget target
        -Long targetId
        -String filePath
        -String mimeType
        -long sizeBytes
    }

    Institution "1" --> "*" GenerationSource
    Institution "1" --> "*" CharacterizationStudy
    CharacterizationStudy "1" *-- "8" SamplingDay
    CharacterizationStudy "1" --> "16" SamplingShift
    SamplingShift "1" *-- "0..1" ShiftPopulation
    SamplingShift "1" *-- "*" WasteBag
    SamplingShift "1" *-- "0..1" Quartering
    SamplingShift "1" *-- "0..1" DensityMeasurement
    Quartering "1" *-- "*" QuarteringResult
    QuarteringResult ..> WasteCategory
    WasteBag ..> GenerationSource
    VerifiableRecord <|-- ShiftPopulation
    VerifiableRecord <|-- WasteBag
    VerifiableRecord <|-- Quartering
    VerifiableRecord <|-- DensityMeasurement
    VerifiableRecord *-- VerificationInfo
    Correction ..> VerifiableRecord
    Crew "1" --> "*" User
    AlertFactory ..> Alert : creates
```

### Value objects and enums

| Type | Kind | Purpose |
| --- | --- | --- |
| `GeoPoint`, `GeoPolygon` | record | WGS84 coordinates and closed polygons of the sources (RN-004) |
| `BagLabel` | record | Bag label `AU-D3-M`, `AU-D3-M-2` (RN-031) |
| `CharacterizationSettings` | record | Sample range 45–55 kg, 2 % tolerance, bag plausibility limits (D8, RN-037) |
| `StudyReopening` | record | Who reopened a closed study, why and when (RN-017) |
| `WasteGroup` | enum | BLANCO, VERDE, NEGRO, SIN_COLOR (Res. 2184 de 2019) |
| `StudyStatus` | enum | PLANIFICADO → EN_EJECUCION → EN_DEPURACION → CERRADO |
| `UserRole`, `Permission` | enum | DOCENTE and ESTUDIANTE_UNIVERSITARIO; permission matrix RN-081 |
| `SourceType`, `ShiftType`, `ShiftStatus`, `VerificationStatus`, `QualityStatus`, `AlertType`, `EvidenceTarget` | enum | Closed lists of the domain |

## 2. Validators

All validators are `final`, stateless classes with a private constructor and `static` methods. Data about other objects (existing codes, other studies, participating sources) is received as parameters, because the domain does not query the database.

```mermaid
classDiagram
    direction LR
    class AccessPolicy { +require(User, Permission)$ +can(User, Permission)$ boolean }
    class InstitutionValidator { +validate(Institution)$ }
    class GenerationSourceValidator { +validate()$ +validateUniqueCode()$ +validateDeletion()$ +validateAvailableForRegistration()$ }
    class WasteCategoryValidator { +validate()$ +validateUniqueName()$ +validateGroupChange()$ +validateDeletion()$ }
    class UserValidator { +validate()$ +validateUniqueEmail()$ }
    class CrewValidator { +validate()$ }
    class StudyValidator { +validate()$ +validateNoOverlap()$ +validateStartExecution()$ +validateClose()$ }
    class ShiftValidator { +validateNewShift()$ +validatePopulation()$ +validateCanRegisterBag()$ +validateCanClose()$ +validateReopen()$ }
    class WasteBagValidator { +validate()$ +isAtypical()$ boolean }
    class QuarteringValidator { +validate()$ +validateCategories()$ +validateNoExistingQuartering()$ +validatePermissions()$ +validateAnnul()$ }
    class DensityValidator { +validate()$ +validateNoExistingDensity()$ }
    class VerificationValidator { +validateStudyAllowsVerification()$ +validateVerifier()$ +validateCorrection()$ }
    class EvidenceValidator { +validate()$ }
    QuarteringValidator ..> AccessPolicy
    VerificationValidator ..> AccessPolicy
```

## 3. GRASP and SOLID traceability

| Principle | Where | Why |
| --- | --- | --- |
| **Information Expert** | `WasteBag.getNetWeight()`, `Quartering.evaluateQuality()`, `SamplingShift.calculateTotalNetWeight()`, `CharacterizationStudy.overlaps()` | Each calculation lives in the class that owns the data |
| **Creator** | `SamplingShift.addBag()` assigns the `BagLabel`; `AlertFactory` creates `Alert` | The aggregate that contains or records the object creates it |
| **Pure Fabrication** | All `*Validator` classes and `AccessPolicy` | Validation rules do not belong to any single entity |
| **Low Coupling** | `model` imports only `exceptions`; `validation` imports only `model` and `exceptions` | Layers can change and be tested in isolation |
| **High Cohesion** | One validator per aggregate (study, shift, bag, quartering…) | Each class has one focused job |
| **Polymorphism** | `VerifiableRecord` (abstract) with `ShiftPopulation`, `WasteBag`, `Quartering`, `DensityMeasurement` | The verification inbox treats every record the same way |
| **SRP** | `VerificationInfo` only handles double registration; `CharacterizationSettings` only holds parameters | One reason to change per class |
| **OCP** | New verifiable records extend `VerifiableRecord`; new permissions are added to `Permission` and `UserRole` | Behavior is extended without modifying existing classes |
| **LSP** | Any `VerifiableRecord` subclass can replace the base type in `VerificationValidator` | Subclasses keep the base contract |
| **DIP** | Services (specs 002–003) will depend on repository interfaces | High-level policy does not depend on storage details |
