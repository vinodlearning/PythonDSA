# NLP System Architecture Diagrams

## 1. System Architecture Overview

```mermaid
graph TB
    subgraph "Frontend Layer"
        UI[User Interface]
        AB[Action Button]
        CB[Custom Button]
    end
    
    subgraph "Integration Layer"
        UAH[Main Handler]
        UAR[Response Object]
        CH[Custom Handler]
    end
    
    subgraph "NLP Processing Layer"
        SJP[NLP Processor]
        WD[Word Database]
        CM[Business Model]
        CP[Custom Processor]
    end
    
    subgraph "Data Layer"
        ATP[Data Provider]
        DB[(Database)]
        CD[(Custom DB)]
    end
    
    UI --> AB
    AB --> UAH
    UAH --> SJP
    UAH --> CM
    SJP --> WD
    CM --> WD
    UAH --> ATP
    ATP --> DB
    UAH --> UAR
    UAR --> UI
```

## 2. NLP Engine Processing Flow

```mermaid
sequenceDiagram
    participant User
    participant UI
    participant UAH as Main Handler
    participant WD as Word Database
    participant CM as Business Model
    participant SJP as NLP Processor
    participant ATP as Data Provider
    participant UAR as Response Object
    participant Custom as Custom Component

    User->>UI: Enter text query
    UI->>UAH: processUserInput(input)
    
    UAH->>WD: preprocessInput(input)
    WD-->>UAH: normalized input
    
    UAH->>CM: processQuery(normalized input)
    CM-->>UAH: business validation result
    
    UAH->>SJP: getQueryResult(normalized input)
    SJP->>WD: spell correction & normalization
    WD-->>SJP: corrected text
    SJP-->>UAH: QueryResult object
    
    UAH->>Custom: customProcessing(queryResult)
    Custom-->>UAH: custom results
    
    UAH->>ATP: executeDataAction(queryResult)
    ATP-->>UAH: data results
    
    UAH->>UAR: createResponse()
    UAR-->>UAH: response object
    
    UAH-->>UI: complete response
    UI-->>User: display results
```

## 3. Component Interaction Diagram

```mermaid
graph LR
    subgraph "Input Processing"
        A[User Input] --> B[Word Database Preprocessing]
        B --> C[Spell Correction]
        C --> D[Business Term Mapping]
        D --> E[Custom Input Processing]
    end
    
    subgraph "NLP Analysis"
        E --> F[NLP Processor]
        F --> G[Entity Extraction]
        F --> H[Query Classification]
        F --> I[Action Type Detection]
        I --> J[Custom Analysis]
    end
    
    subgraph "Business Validation"
        E --> K[Business Model]
        K --> L[Business Rules Validation]
        K --> M[Field Mapping]
        K --> N[Query Type Detection]
        N --> O[Custom Validation]
    end
    
    subgraph "Response Generation"
        G --> P[Response Object]
        H --> P
        I --> P
        L --> P
        M --> P
        N --> P
        J --> P
        O --> P
        P --> Q[SQL Query Generation]
        P --> R[JSON Response]
        P --> S[Custom Response Format]
    end
```

## 4. Data Flow Architecture

```mermaid
flowchart TD
    A[User Input Text] --> B{Input Validation}
    B -->|Valid| C[Word Database Processing]
    B -->|Invalid| D[Error Response]
    
    C --> E[Business Model Logic]
    C --> F[NLP Processor]
    
    E --> G[Business Validation]
    F --> H[Entity Extraction]
    
    G --> I[Query Type Classification]
    H --> I
    
    I --> J{Query Type}
    J -->|Type A| K[Processing A]
    J -->|Type B| L[Processing B]
    J -->|Type C| M[Processing C]
    J -->|Type D| N[Processing D]
    
    K --> O[Data Provider]
    L --> O
    M --> O
    N --> O
    
    O --> P[Response Object]
    P --> Q[SQL Generation]
    P --> R[JSON Response]
    P --> S[UI Display]
```

## Instructions for Word Document:

1. **Copy each diagram** by selecting the code block and copying it
2. **Paste into Word** - Word should automatically render the Mermaid diagrams
3. **If Word doesn't render Mermaid:**
   - Use online Mermaid editor: https://mermaid.live
   - Paste the code and export as PNG/SVG
   - Insert the image into Word

## Easy Class Name Replacement:

When you're ready to customize, just replace these generic terms:
- `Main Handler` → Your actual handler class name
- `NLP Processor` → Your NLP engine class name
- `Business Model` → Your business logic class name
- `Data Provider` → Your data access class name
- `Response Object` → Your response class name
- `Custom Component` → Your specific custom classes 