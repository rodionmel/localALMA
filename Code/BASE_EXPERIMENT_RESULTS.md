# Base Experiment Results

## Experiment Setup

**Hypothesis**: Queries collected from CharacteristicSetGeneratingOracle during active learning are sufficient to learn an equivalent M2MA using PassiveLearningOracle.

**Test**: 
- Phase 1: Learn M2MA from DFA using CharacteristicSetGeneratingOracle (active learning)
- Collect all queries made during Phase 1
- Phase 2: Learn M2MA from collected queries using PassiveLearningOracle (passive learning)
- Compare results

---

## Phase 1: CharacteristicSetGeneratingOracle (Active Learning)

**Input**: DFA from `F1_last_2_is_a_ab_dfa.dot`
- 4 states, 2 accepting states
- Language: Words where 2nd character from end is 'a'
- Alphabet: [a, b]

**Results**:
- ✅ **Learned dimension**: 3
- ✅ **Runtime**: ~0.12 seconds
- ✅ **Queries collected**: 26
  - Positive answers: 16
  - Negative answers: 10
- ✅ **Output file**: `F1_last_2_is_a_ab_dfa_queries.json`

**Status**: ✅ **SUCCESS** - Successfully learned M2MA from DFA

---

## Phase 2: PassiveLearningOracle (Passive Learning)

**Input**: `F1_last_2_is_a_ab_dfa_queries.json` (26 queries from Phase 1)
- 16 positive examples
- 10 negative examples
- Alphabet: [a, b]

**Results**:
- ⚠️ **Learned dimension**: 23 (different from Phase 1's dimension 3)
- ✅ **Runtime**: ~0.08 seconds
- ✅ **Examples used**: 26 (all from Phase 1)

**Status**: ⚠️ **DIFFERENT RESULT** - Learned different M2MA

---

## Comparison Results

### Dimension Comparison
- Phase 1 dimension: **3**
- Phase 2 dimension: **23**
- **Result**: ✗ **Dimensions differ significantly**

### Verification Against Collected Queries
- Phase 2 M2MA tested on all 26 collected queries
- **Result**: ✅ **Perfect match** (26/26 correct)
- All queries from Phase 1 are correctly classified by Phase 2 M2MA

### Verification Against Original DFA
- Phase 2 M2MA tested on 100 random words
- **Result**: ✗ **37 mismatches found** (63/100 correct, 37% error rate)

**Example Mismatches**:
- `"a b"` - DFA accepts (1), Phase 2 M2MA rejects (0) ❌
- `"b a b"` - DFA accepts (1), Phase 2 M2MA rejects (0) ❌
- `"a a a a b"` - DFA accepts (1), Phase 2 M2MA rejects (0) ❌

---

## Key Findings

### 1. Hypothesis Status: ❌ **DISPROVEN**

The queries collected from CharacteristicSetGeneratingOracle are **NOT sufficient** to learn an equivalent M2MA using PassiveLearningOracle.

### 2. What Works
- ✅ Phase 1 successfully learns correct M2MA (dimension 3)
- ✅ Phase 2 correctly classifies all 26 collected queries
- ✅ Counterexamples are now properly captured

### 3. What Doesn't Work
- ❌ Phase 2 learns a different M2MA (dimension 23 vs 3)
- ❌ Phase 2 fails on words not in the collected set (37% error rate)
- ❌ The 26 queries do not form a characteristic set sufficient for passive learning

### 4. Why This Happens

**Active Learning (Phase 1)**:
- Algorithm can ask targeted queries
- Can refine hypothesis based on counterexamples
- Only needs 26 queries to converge to correct M2MA

**Passive Learning (Phase 2)**:
- Only has the 26 fixed examples
- Cannot ask new queries
- Learns an M2MA that fits those 26 examples perfectly
- But this M2MA is **overfitted** - it doesn't generalize to the full language

### 5. The Overfitting Problem

Phase 2's M2MA (dimension 23) is much larger than Phase 1's (dimension 3), suggesting:
- It's memorizing the 26 examples rather than learning the underlying pattern
- The 26 examples are not sufficient to constrain the learning to the correct language
- More examples or a different learning approach is needed

---

## Conclusion

**The base experiment disproves the hypothesis.**

The queries collected during active learning (26 queries) are:
- ✅ Sufficient for active learning to converge to correct M2MA
- ❌ **NOT sufficient** for passive learning to learn equivalent M2MA

**Implications**:
1. Active learning queries may not form a characteristic set
2. Passive learning needs more examples or different examples
3. The learning algorithm may need modification for passive learning
4. Counterexamples are important but not sufficient alone

---

## Next Steps

Possible directions:
1. **Add more examples**: Use variations (shuffling + redundancy) to test if more examples help
2. **Analyze queries**: Understand what queries are missing
3. **Modify learning**: Adapt the learning algorithm for passive learning
4. **Test with larger DFAs**: See if the problem scales

---

**Experiment Date**: December 2024  
**DFA**: F1_last_2_is_a_ab_dfa.dot (4 states)  
**Status**: Hypothesis disproven

