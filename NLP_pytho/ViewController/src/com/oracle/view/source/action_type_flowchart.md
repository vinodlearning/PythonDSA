# Action Type Selection Logic (StandardJSONProcessor)

```mermaid
flowchart TD
    A[Start: User Query] --> B{Update/Command Verb?}
    B -- Yes, Parts Context & Price --> C1[UPDATE_PART_PRICE]
    B -- Yes, Parts Context & Not Price --> C2[UPDATE_PART]
    B -- Yes, Not Parts Context --> C3[UPDATE_CONTRACT]
    B -- No --> D{Parts/Part Attr/Contract Number/Part Number?}
    D -- Parts & Contract# & Part# --> E1[PARTS_BY_CONTRACT_NUMBER]
    D -- Parts & Part# --> E2[PARTS_BY_PART_NUMBER]
    D -- Parts & Contract# --> E3[PARTS_BY_CONTRACT_NUMBER]
    D -- Parts Only --> E4[PARTS_BY_FILTER]
    D -- Not Parts & Contract# & Not Part# --> F1[CONTRACT_BY_CONTRACT_NUMBER]
    D -- Filters & Not Contract# --> F2[CONTRACT_BY_FILTER]
    D -- Customer# & Not Contract# --> F3[CONTRACT_BY_FILTER]
    D -- Contract Context & Special Filter Words --> F4[CONTRACT_BY_FILTER]
    D -- Contract Context Only --> F5[CONTRACTS_LIST]
    D -- Create Contract + (steps/help/how) --> G1[HELP_CREATE_USER]
    D -- Create Contract Intent --> G2[HELP_CREATE_BOT]
    D -- Contract# & Not Create/Update Intent --> F1
    D -- None of Above --> H[CONTRACTS_SEARCH]

    %% User case mapping
    subgraph UserCases
      UC1["update price of item GHI789 in 778899"]
      UC2["show me part NSN456 details from 789012"]
      UC3["list all active parts"]
      UC4["show project type,status for 123456"]
      UC5["list contracts with MOQ > 100"]
      UC6["how to create contract?"]
    end
    UC1 --> C1
    UC2 --> E1
    UC3 --> E4
    UC4 --> F1
    UC5 --> F2
    UC6 --> G1
```

**Legend:**
- Each node represents a possible action type.
- Diamonds represent decision points in the logic.
- User cases (bottom) are mapped to the action type node they trigger.

**How to view:**
- Use a Mermaid live editor or compatible Markdown viewer to see the rendered diagram. 