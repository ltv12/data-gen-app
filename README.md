# Data generator

## Context

Application aims to help with synthetic data generation based on a simple configuration.

```yaml
definitions:
  - name: events
    fields:
      - name: "id"
        type: uuid
      - name: "created_at"
        type: timestamp
      - name: "created_by"
        type: string
        possible_values: [ "system", "igor", "juris" ]
      - name: "country"
        type: string
        possible_values: [ "Russia", "Litva" ]
      - name: "amount"
        type: numeric 
```

## Points to improvements

- FieldSpec type parsed only if in lower case (ideally to make case insensitive)
- Add DSL validation
- Add more types
- Complex types Map/List
- Stateful types as monotonically increasing ID
- Add sampling for commonly used fields (first name, last name, gender, country and eg)
- Externalise rate limiter configuration
- Separate write logic (possibly for different types from Generator App)
- Much more ….
## Aim of the project

Following points is to use techniques and libraries:

- circe to work with JSON
- circe-yaml to work with YAML
- cats-core to use syntax
- cats-effects to organize rate limiter logic.

## Useful resources

- Randoms for specific types https://github.com/typelevel/scalacheck
- Sampler for spcefic data (city/language/name/last name) - https://github.com/hmrclt/stub-data-generator
