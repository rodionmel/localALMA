# Experiment Status Report

## Base Experiment Run - Status

### Phase 1: CharacteristicSetGeneratingOracle ✓ COMPLETED

**Configuration:**
- DFA File: `test_examples/F1_last_2_is_a_ab_dfa.dot`
- DFA: 4 states, 2 accepting states
- Alphabet: [a, b]
- Start state: 2
- EQ Settings: maxTestLen=25, numTests=1000

**Results:**
- ✅ Learned dimension: **3**
- ✅ Runtime: ~0.12 seconds
- ✅ Total queries collected: **813-828** (varies by run)
- ✅ Positive answers: **401-411**
- ✅ Negative answers: **402-427**
- ✅ Queries output to: `test_examples/F1_last_2_is_a_ab_dfa_queries.json`

**Status**: Phase 1 completed successfully ✓

---

### Phase 2: PassiveLearningOracle ⏳ IN PROGRESS / HANGING

**Configuration:**
- Input: `test_examples/F1_last_2_is_a_ab_dfa_queries.json`
- Examples loaded: 401-411 positive, 402-427 negative
- Closed-world assumption: **true** (unknown words treated as negative)

**Status**: 
- ⚠️ **HANGING** - Learning process starts but does not complete
- Timeout after 5 minutes (300 seconds)
- No error messages, just hangs after "Starting learning process..."

**Possible Issues:**
1. Learning algorithm may be making many queries
2. EQ method might be slow with 800+ examples
3. Algorithm might be stuck in a loop
4. Memory or performance issue with large example set

**Next Steps:**
1. Add progress/debug output to identify where it's hanging
2. Try with smaller example set first
3. Check if EQ is being called repeatedly
4. Verify PassiveLearningOracle.MQ performance

---

## Observations

1. **Phase 1 works correctly**: DFA oracle successfully learns M2MA and collects queries
2. **Query collection works**: JSON file is created with all queries
3. **Phase 2 hangs**: Learning with PassiveLearningOracle doesn't complete

## Hypothesis Status

**Current Status**: ⏳ **INCOMPLETE** - Cannot verify hypothesis yet

- Phase 1: ✅ Successfully collected queries
- Phase 2: ⏳ Cannot complete - hangs during learning
- Comparison: ❌ Cannot compare (Phase 2 incomplete)

## Recommendations

1. **Debug Phase 2**: Add logging to identify where it hangs
2. **Test with smaller set**: Try with fewer examples first
3. **Check EQ performance**: Verify PassiveLearningOracle.EQ() isn't too slow
4. **Alternative approach**: Consider using a different EQ strategy for passive learning

---

**Last Updated**: December 24, 2024
**Experiment File**: `DFAToPassiveLearningExperiment.java`
**Test DFA**: `F1_last_2_is_a_ab_dfa.dot`

