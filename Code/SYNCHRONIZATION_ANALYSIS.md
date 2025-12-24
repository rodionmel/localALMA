# Synchronization Analysis: CharacteristicSetGeneratingOracle vs PassiveLearningOracle

## The Question

What is required for PassiveLearningOracle to learn **exactly the same M2MA** as CharacteristicSetGeneratingOracle?

## Current Situation

### CharacteristicSetGeneratingOracle (Active Learning)
- **MQ**: Answers queries from learning algorithm, records them
- **EQ**: Uses statistical testing (random strings), finds shortest counterexample
- **Learning path**: Depends on which counterexamples are found
- **Queries collected**: Only MQ queries + counterexamples from EQ

### PassiveLearningOracle (Passive Learning)
- **MQ**: Answers based on fixed example set
- **EQ**: Checks ALL examples, finds shortest counterexample
- **Learning path**: Depends on order of examples and which counterexamples are found
- **Examples**: Fixed set from JSON file

## Key Differences

### 1. EQ Strategy Difference

**CharacteristicSetGeneratingOracle.EQ()**:
- Generates random test strings
- Tests hypothesis against DFA
- Finds shortest counterexample among random tests
- May not find a counterexample even if one exists (statistical)

**PassiveLearningOracle.EQ()**:
- Tests hypothesis against ALL examples in the set
- Finds shortest counterexample among examples
- Guaranteed to find counterexample if hypothesis is wrong (complete check)

### 2. Query Collection

**CharacteristicSetGeneratingOracle** collects:
- MQ queries made during learning
- Counterexamples from EQ (when found)

**PassiveLearningOracle** uses:
- Fixed set of examples from JSON
- No new queries can be made

## What's Missing for Synchronization

### Problem 1: Different EQ Behavior

Even with the same queries, the EQ methods behave differently:

1. **CharacteristicSetGeneratingOracle**: Random statistical testing
   - May test 1000 random strings
   - Finds shortest among those tested
   - May miss counterexamples that exist

2. **PassiveLearningOracle**: Complete example checking
   - Tests all examples in the set
   - Finds shortest among examples
   - Guaranteed to find if example set contains counterexample

**Solution**: Make PassiveLearningOracle use the same statistical EQ strategy?

### Problem 2: Query Order and Timing

The learning algorithm's behavior depends on:
- **When** queries are made
- **What** counterexamples are found
- **Order** of counterexamples

**CharacteristicSetGeneratingOracle**:
- Queries are made on-demand during learning
- Counterexamples are found via random statistical testing
- Learning path depends on which random counterexamples are found

**PassiveLearningOracle**:
- All examples are available from the start
- Counterexamples are found by checking all examples
- Learning path depends on which examples are counterexamples

**Solution**: Need to ensure same counterexamples are found in same order?

### Problem 3: Missing Queries

The collected queries might not include all queries needed:

1. **Composite queries**: `MQ(addStrings(row, col))` - these are computed during learning
2. **Counterexample queries**: Counterexamples are added to observation table, then queried
3. **Timing**: Some queries might be made before counterexample is added

**Current collection**:
- MQ queries: ✅ Collected
- Counterexamples: ✅ Collected (after fix)
- Composite queries: ✅ Collected (when MQ is called)

But are they in the right order? Are all necessary queries present?

## Requirements for Full Synchronization

### Option 1: Exact Query Sequence

For PassiveLearningOracle to learn the same M2MA, it would need:

1. **Exact same queries** in **exact same order** as Phase 1
2. **Same EQ behavior**: Use statistical EQ (not complete example check)
3. **Same counterexamples**: Find the same counterexamples in the same order

**Challenge**: This requires:
- Recording the exact sequence of queries
- Recording which counterexamples were found when
- Using statistical EQ in PassiveLearningOracle (defeats purpose of passive learning)

### Option 2: Characteristic Set

For PassiveLearningOracle to learn equivalent M2MA, it needs:

1. **Characteristic set**: A set of queries that uniquely identifies the language
2. **Complete coverage**: All distinguishing strings
3. **Sufficient examples**: Enough to constrain learning to correct M2MA

**Current problem**: The 15-26 queries collected are NOT a characteristic set

### Option 3: Synchronized EQ

Make PassiveLearningOracle use the same EQ strategy:

1. **Statistical EQ**: Instead of checking all examples, use random testing
2. **Same random seed**: Use same seed to generate same test strings
3. **Same counterexamples**: Find same counterexamples in same order

**Implementation**:
- Add statistical EQ option to PassiveLearningOracle
- Use same random seed as CharacteristicSetGeneratingOracle
- Generate same test strings
- Find same counterexamples

## Proposed Solution: Synchronized Statistical EQ

### Changes Needed

1. **PassiveLearningOracle.EQ()**: Add option to use statistical testing
2. **Random seed**: Use fixed seed or same seed as Phase 1
3. **Test generation**: Use same `genTest()` calls
4. **Counterexample selection**: Use same "shortest" strategy

### Code Changes

```java
// In PassiveLearningOracle
public static boolean EQ(...) {
    if (useStatisticalEQ) {
        // Use same statistical EQ as CharacteristicSetGeneratingOracle
        // with same random seed
        return EQStatistical(...);
    } else {
        // Current: check all examples
        return EQComplete(...);
    }
}
```

### Benefits

- Same EQ behavior in both phases
- Same counterexamples found
- Same learning path
- Should produce same M2MA

### Drawbacks

- Defeats purpose of "passive" learning (becomes active again)
- Requires random seed synchronization
- More complex

## Alternative: Record Full Learning Trace

Instead of just queries, record:

1. **Query sequence**: Order of all MQ calls
2. **Counterexample sequence**: Order of counterexamples found
3. **Observation table state**: State after each counterexample
4. **EQ test strings**: All random test strings generated (for reproducibility)

Then replay this trace in PassiveLearningOracle.

## Recommendation

For **exact synchronization**, you need:

1. **Same EQ strategy**: Both use statistical EQ with same seed
2. **Same query sequence**: Process queries in same order
3. **Same counterexamples**: Find same counterexamples in same order

For **equivalent learning** (different M2MA but equivalent language):

1. **Characteristic set**: Need sufficient distinguishing queries
2. **Complete example set**: All necessary examples to constrain learning

The current approach (collecting queries) is trying for option 2, but the collected queries are not sufficient.

