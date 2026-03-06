You are a senior Java engineer working on FormKiQ.

Generate a Java 17 source file using the “FormKiQ DynamoDB record + Builder” structure used by DocumentWorkflowRecord:

Requirements
1) Output must be a single Java code block containing the full source file.
2) Use a Java `record` named: <ENTITY_NAME>Record
3) Include:
    - package: <PACKAGE_NAME>
    - imports (only what is used)
    - record fields: `DynamoDbKey key` + the provided fields (in the same order as input)
    - canonical constructor:
        - Objects.requireNonNull for required fields (see field list)
        - defensive copies for Date fields (and optionally Lists/Maps if specified)
    - static attribute-name constants: ATTR_<FIELD_NAME_UPPER_SNAKE> for each persisted field
    - `public static <ENTITY_NAME>Record fromAttributeMap(Map<String, AttributeValue> attributes)`
        - return null if attributes is null/empty
        - use DynamoDbKey.fromAttributeMap(attributes)
        - use DynamoDbTypes.toString/toBoolean/toInteger/toDate/etc for conversions
        - enums: EnumType.valueOf(string)
        - maps: if Map<String,String> then convert AttributeValue.m() to map of s()
        - Map<String,Object> parameters: use AttributeValueToMap if requested
    - `public Map<String, AttributeValue> getAttributes()`
        - start with key.getAttributesBuilder()
        - write each field using withString/withBoolean/withInteger/withDate/withMap...
        - only include optional fields if non-null (unless explicitly “required”)
    - `public static Builder builder()`
    - nested `public static class Builder implements DynamoDbEntityBuilder<<ENTITY_NAME>Record>`
        - private fields + sensible defaults if specified
        - `build(siteId)` calls validate(), buildKey(siteId), returns record
        - `buildKey(siteId)` computes pk/sk and optional GSIs from the provided key spec
        - fluent setters for every field with full Javadoc
        - `private Builder validate()` using ValidationBuilder, check required rules
4) All methods and constants must have Checkstyle-friendly Javadocs:
    - summary sentence
    - @param and @return descriptions everywhere
5) Do NOT use DynamodbRecord interface in the new code.
6) No extra commentary outside the code block.

Inputs
ENTITY_NAME: <ENTITY_NAME>
PACKAGE_NAME: <PACKAGE_NAME>

FIELDS (one per line):
- <name>: <type> [required|optional] [persisted|transient] [default=<value>] [notes=...]
  Examples:
    - documentId: String required persisted
    - status: WorkflowStatus required persisted
    - insertedDate: Date required persisted default=new Date()
    - metadata: Map<String,String> optional persisted
    - parameters: Map<String,Object> optional persisted useAttributeValueToMap=true
    - workflowId: String optional transient

KEY_SPEC:
- PK: <string expression>  (use siteId with DynamoDbKey.builder().pk(siteId, pk))
- SK: <string expression>
- GSI1_PK: <string expression or null>
- GSI1_SK: <string expression or null>
- GSI2_PK: <string expression or null>
- GSI2_SK: <string expression or null>
  Notes:
- If any GSI key is null, omit setting it on DynamoDbKey.Builder.
- Use createDatabaseKey(siteId, ...) only if explicitly requested; otherwise use plain prefixes like "wfdoc#".

Now generate the code for the given entity using these inputs.

---BEGIN INPUT---

ENTITY_NAME: <ENTITY_NAME>
PACKAGE_NAME: <PACKAGE_NAME>

FIELDS:
- ...

KEY_SPEC:
PK: ...
SK: ...
GSI1_PK: ...
GSI1_SK: ...
GSI2_PK: ...
GSI2_SK: ...

---END INPUT---
