# Deterministic Experiment Results

## Setup

**CharacteristicSetGeneratingOracle**: Now uses **deterministic EQ** (lexicographically smallest counterexample)
- No randomness
- Systematic enumeration of all words up to maxTestLen
- Returns lexicographically smallest counterexample

**PassiveLearningOracle**: Still uses **complete example check**
- Checks all examples in the set
- Returns lexicographically smallest counterexample among examples

## Results

### Phase 1: CharacteristicSetGeneratingOracle (Deterministic)
- ✅ **Learned dimension**: 3
- ✅ **Queries collected**: 15 (9 positive, 6 negative)
- ✅ **Deterministic**: Same results every run
- ✅ **Runtime**: 0.15s

### Phase 2: PassiveLearningOracle
- ⚠️ **Learned dimension**: 6 (different from Phase 1)
- ✅ **Examples used**: 15 (same as Phase 1 queries)
- ✅ **Matches all collected queries**: 15/15
- ❌ **Random words**: 69/100 correct (31% error rate)

## Key Findings

### 1. Determinism Achieved
- Phase 1 is now fully deterministic
- Same queries collected every run
- Same learning path every run

### 2. Still Different Results
- Phase 2 learns dimension 6 vs Phase 1's dimension 3
- Phase 2 fails on 31% of random words
- **Hypothesis still disproven**

### 3. Why They're Still Different

**CharacteristicSetGeneratingOracle.EQ()**:
- Enumerates ALL words up to maxTestLen in lexicographic order
- Tests: "", "a", "b", "a a", "a b", "b a", "b b", "a a a", ...
- Finds lexicographically smallest counterexample among ALL possible words

**PassiveLearningOracle.EQ()**:
- Checks only the 15 examples in the JSON file
- Tests: Only the 15 collected queries
- Finds lexicographically smallest counterexample among THOSE 15 examples

**The Problem**:
- Different counterexample pools → Different counterexamples found
- Different counterexamples → Different learning paths
- Different learning paths → Different final M2MAs

## Example

**Phase 1 (CharacteristicSetGeneratingOracle)**:
- Enumerates: "", "a", "b", "a a", "a b", "b a", ...
- Finds counterexample: "a" (lexicographically smallest among ALL words)
- Uses "a" for learning

**Phase 2 (PassiveLearningOracle)**:
- Checks examples: "a a", "a a a", "a a b", "a b", "b a a", ...
- Finds counterexample: "a a" (lexicographically smallest among THOSE 15 examples)
- Uses "a a" for learning (different from Phase 1!)

## What's Needed for Full Synchronization

To make PassiveLearningOracle learn the **same M2MA**, we need:

### Option 1: Same EQ Strategy
Make PassiveLearningOracle use the same deterministic enumeration:
- Enumerate all words up to maxTestLen
- Test against hypothesis
- Return lexicographically smallest counterexample
- **But**: This defeats the purpose of "passive" learning

### Option 2: Include All Necessary Words
The 15 collected queries are not sufficient. We need:
- All words that CharacteristicSetGeneratingOracle tested during EQ
- Or at least all counterexamples that were found
- Or a characteristic set that uniquely identifies the language

### Option 3: Record Full EQ Trace
Instead of just queries, record:
- All words tested during EQ (even if not counterexamples)
- Which counterexamples were found in which order
- The exact learning path

## Current Status

✅ **Determinism**: Achieved in Phase 1
❌ **Synchronization**: Not achieved - Phase 2 still learns different M2MA
❌ **Hypothesis**: Still disproven - 15 queries not sufficient

## Next Steps

1. **Make PassiveLearningOracle use same deterministic EQ**: Enumerate all words, not just examples
2. **Or**: Record all EQ test words, not just counterexamples
3. **Or**: Accept that passive learning needs more/different examples than active learning

The fundamental issue remains: **The 15 queries collected are not a characteristic set sufficient for passive learning**.

