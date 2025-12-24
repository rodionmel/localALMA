# Why EQ Counterexamples Are Not Being Captured

## The Problem

During Phase 1 learning, when `EQ()` finds a counterexample, that counterexample word is **not immediately added** to `allQueries`. Here's why:

## Flow Analysis

### Step 1: EQ Finds Counterexample
```java
// In CharacteristicSetGeneratingOracle.EQ()
for (int i=0; i<EQNumTests; i++) {
    String test = M2MA.genTest(...);  // Generate random test word
    
    int dfaAnswer = simulateDFA(test);  // Direct DFA simulation - NO recording
    int hypothesisAnswer = M2MA.MQArbitrary(...);
    
    if (dfaAnswer != hypothesisAnswer) {
        M2MA.counterExample = test;  // Store counterexample
        return false;  // Return immediately
    }
}
```

**Key Point**: `simulateDFA()` does NOT record the query. The counterexample word is discovered but not added to `allQueries`.

### Step 2: Learning Algorithm Uses Counterexample
```java
// In M2MA.learnMain()
if (!EQ(...)) {
    learnedSize++;
    learnedRowIndices.add(counterExample);  // Add counterexample to indices
    learnedColIndices.add(counterExample);
}
```

The counterexample is added to `learnedRowIndices` and `learnedColIndices`.

### Step 3: Hypothesis Building Queries
```java
// In M2MA.createHypothesisFinalVector()
for (int i=0; i<learnedSize; i++) {
    if (MQ(learnedRowIndices.get(i)) == 1) {  // MQ() IS called here
        // ...
    }
}
```

**Important**: When `MQ(learnedRowIndices.get(i))` is called, it WILL record the query because it goes through `MQ()`, not `simulateDFA()`.

### Step 4: Transition Matrix Building
```java
// In M2MA.createHypothesisTransitionMatrices()
F_xi[j][i] = MQ(addStrings(learnedRowIndices.get(i), learnedColIndices.get(j)));
F_xi_letter[i][j] = MQ(addStrings(addStrings(learnedRowIndices.get(i), letter), learnedColIndices.get(j)));
```

These composite queries ARE recorded.

## The Issue

**Counterexamples ARE eventually captured**, but there's a subtle problem:

1. **Timing**: The counterexample is discovered by `simulateDFA()` (not recorded)
2. **Later**: The counterexample is added to `learnedRowIndices`
3. **Eventually**: `MQ(counterexample)` is called, which DOES record it

**However**, the issue is:
- The counterexample word itself IS queried via `MQ()` later, so it SHOULD be in `allQueries`
- But we only have 20 queries total, which suggests the learning converged very quickly
- The real question is: **Are all the distinguishing words being captured?**

## Why Only 20 Queries?

Looking at the results:
- Phase 1 learned dimension: 3
- Only 20 queries collected
- This suggests the learning algorithm converged quickly

The 20 queries likely include:
- Initial queries to build the observation table
- Counterexample words (queried via MQ when building hypothesis)
- Composite queries (row+column combinations)

## The Real Problem

The issue might not be that counterexamples aren't captured, but rather:

1. **The learning algorithm converged too quickly** - it only needed 20 queries to learn
2. **These 20 queries are not sufficient** - they don't form a characteristic set
3. **The learned M2MA (dimension 3) is correct for those 20 queries** but not for the full language

## Verification

The counterexample words ARE being captured because:
- They're added to `learnedRowIndices`
- `MQ(learnedRowIndices.get(i))` is called, which records them
- They appear in the collected queries

But the collected queries (20 total) are simply **not sufficient** to learn the correct M2MA via passive learning.

## Solution

If we want to ensure counterexamples are captured immediately (not just when queried later), we could:

1. **Record counterexample immediately in EQ()**:
   ```java
   if (dfaAnswer != hypothesisAnswer) {
       M2MA.counterExample = test;
       // Also record it immediately
       allQueries.put(test, dfaAnswer);
       totalQueries++;
       return false;
   }
   ```

2. **But this might be redundant** since the counterexample will be queried via MQ() later anyway.

The real issue is that **20 queries are not enough** to learn the language via passive learning, even though they were sufficient for active learning.

