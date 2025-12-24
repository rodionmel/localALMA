# Remaining Sources of Randomness

## Analysis

After making CharacteristicSetGeneratingOracle.EQ() deterministic, let's check for remaining randomness:

### ✅ Removed Randomness

1. **CharacteristicSetGeneratingOracle.EQ()**: ✅ **DETERMINISTIC**
   - No more `Math.random()`
   - Uses systematic lexicographic enumeration
   - Returns lexicographically smallest counterexample

2. **PassiveLearningOracle.EQ()**: ✅ **DETERMINISTIC**
   - No randomness
   - Checks examples in fixed order
   - Returns lexicographically smallest counterexample

### ⚠️ Remaining Randomness (Non-Critical)

1. **M2MA.genTest()**: Still uses `Math.random()`
   - **Location**: `M2MA.java:1045, 1048`
   - **Usage**: Only used in verification/testing code, NOT in learning
   - **Impact**: Does NOT affect learning process
   - **Used in**:
     - `DFAToPassiveLearningExperiment.verifyAgainstDFA()` - for random word testing
     - `VerifyEquivalence.testRandomWords()` - for verification
     - NOT used in actual learning algorithm

2. **ExperimentUtilities.shuffleSamples()**: Uses `Random`
   - **Location**: `ExperimentUtilities.java:30-31`
   - **Usage**: Only for experiment variations (shuffling)
   - **Impact**: Only affects experiments with variations enabled
   - **Not used in base experiment**

### ✅ Learning Process is Deterministic

**Core Learning Algorithm**:
- ✅ CharacteristicSetGeneratingOracle.EQ(): Deterministic (lexicographic enumeration)
- ✅ PassiveLearningOracle.EQ(): Deterministic (checks examples in order)
- ✅ M2MA.learn(): Deterministic (no randomness in algorithm logic)
- ✅ Query collection: Deterministic (same queries every run)

**The learning process itself is now fully deterministic!**

## Verification

The only remaining randomness is in:
1. **Verification/testing code** - generates random words for testing, doesn't affect learning
2. **Experiment variations** - shuffling for testing robustness, not used in base experiment

## Conclusion

✅ **No randomness in the learning process**
- Phase 1 learning: Fully deterministic
- Phase 2 learning: Fully deterministic
- Query collection: Fully deterministic

The variation you might see is due to:
- Different EQ strategies (CharacteristicSetGeneratingOracle vs PassiveLearningOracle)
- Different counterexample pools
- NOT due to randomness

## Summary

| Component | Randomness | Affects Learning? |
|-----------|-----------|-------------------|
| CharacteristicSetGeneratingOracle.EQ() | ❌ None | N/A |
| PassiveLearningOracle.EQ() | ❌ None | N/A |
| M2MA.learn() | ❌ None | N/A |
| M2MA.genTest() | ✅ Yes | ❌ No (only for verification) |
| ExperimentUtilities.shuffle() | ✅ Yes | ❌ No (only for variations) |

**Answer: No randomness left in the learning process!**

