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
          possible_values: ["system", "igor", "juris"]
        - name: "country"
          type: string
          possible_values: ["Russia", "Litva"]
        - name: "amount"
          type: numeric 
```

## Schedule

* Dec 29 - describe a data unit (logical unit of data to generate) configuration and implement mapping from configuration to Map

//TODO: support nested objects

* Dec 30 - describe a gen config (output configuration) and implement it
  * output configuration for json format
  * output configuration for avro format
* Dec 31 - config that describes rate of outcomming data
* Jun 1  - Output data to filesystem 
* Jun 2  - Output data to Kinesis 


## Points to improvments
- FieldSpec type parsed only if in lower case (ideally to make case insensitive)
- Added Complex Types as Map/Array
- DSL validation. 

## Aim of the project
Following points is to use techniques and libraries:
- ADTs


## Things that I learned during the project implementation. 


## Useful resources

- Randoms for specific types https://github.com/typelevel/scalacheck
- Sampler for spcefic data (city/language/name/last name) - https://github.com/hmrclt/stub-data-generator
