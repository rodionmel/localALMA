# Why MQ Calls Differ Between Phase 1 and Phase 2

## Observation

**Phase 1 MQ calls (first 4)**:
1. "" -> 0
2. a -> 0
3. b -> 0
4. a a -> 1 (cached)

**Phase 2 MQ calls (first 4)**:
1. "" -> 0
2. a -> 0
3. b -> 0
4. a b -> 1

The first 3 calls are **identical**, but call #4 differs!

## Why This Happens

### Initial Setup (Same)
The first 3 queries are the same because:
- Both phases start with the same initial observation table
- Initial queries: "", "a", "b" (to build initial table)
- These are deterministic

### First EQ Call (Different Counterexamples)
After the initial queries, the algorithm calls EQ():

**Phase 1 (CharacteristicSetGeneratingOracle)**:
- Enumerates all words lexicographically: "", "a", "b", "a a", "a b", ...
- Tests hypothesis against DFA
- Finds first counterexample: **"a a"** (lexicographically smallest)
- Adds "a a" to `learnedRowIndices` and `learnedColIndices`

**Phase 2 (PassiveLearningOracle)**:
- Checks only the 15 examples in JSON
- Tests hypothesis against examples
- Finds first counterexample: **"a b"** (shortest among examples)
- Adds "a b" to `learnedRowIndices` and `learnedColIndices`

### Query #4 (Different Because of Different Counterexamples)

After the first EQ, the algorithm needs to build the hypothesis transition matrices. This requires queries of the form:
- `MQ(row + col)` for all row/col pairs
- `MQ(row + letter + col)` for all row/letter/col combinations

**Phase 1** (with "a a" added):
- Observation table now has: "", "a", "b", **"a a"**
- Query #4: `MQ("" + "a a")` = `MQ("a a")` → 1 (cached, already queried)

**Phase 2** (with "a b" added):
- Observation table now has: "", "a", "b", **"a b"**
- Query #4: `MQ("" + "a b")` = `MQ("a b")` → 1

## Root Cause

The difference in **call #4** is because:

1. **Different counterexamples** from first EQ:
   - Phase 1: "a a"
   - Phase 2: "a b"

2. **Different observation tables**:
   - Phase 1: { "", "a", "b", "a a" }
   - Phase 2: { "", "a", "b", "a b" }

3. **Different composite queries**:
   - Phase 1 queries: `MQ("" + "a a")`, `MQ("a" + "a a")`, etc.
   - Phase 2 queries: `MQ("" + "a b")`, `MQ("a" + "a b")`, etc.

## The Cascade Effect

Once the counterexamples differ, everything cascades:

1. Different counterexample → Different observation table
2. Different observation table → Different composite queries
3. Different queries → Different hypothesis
4. Different hypothesis → Different next counterexample
5. Repeat...

This is why:
- Phase 1: 3 EQ calls, 115 MQ calls
- Phase 2: 6 EQ calls, 48 MQ calls

## Why Same Learner Makes Different Queries

The learner algorithm is **deterministic** and **the same**, but:

1. **It queries based on current observation table state**
2. **Observation table state depends on counterexamples found**
3. **Counterexamples depend on EQ strategy**
4. **Different EQ strategies → Different counterexamples → Different queries**

## Conclusion

The learner is the same, but it's **reactive** - it makes queries based on:
- Current observation table state
- Which counterexamples were found
- Current hypothesis

Since Phase 1 and Phase 2 find different counterexamples (due to different EQ strategies), they build different observation tables, leading to different query sequences.

**The learner is deterministic, but its behavior depends on the oracle's responses!**

