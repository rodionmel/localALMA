# Experiment Variations

## Overview

After running the base experiment, you can test harder variations to verify robustness:

1. **Random Reordering**: Shuffle positive and negative samples
2. **Redundant Examples**: Add more random examples (longer strings) based on DFA acceptance

## Variation 1: Random Reordering

### Purpose
Test if the order of examples matters for learning. A robust learning algorithm should be order-independent.

### Implementation
- Randomly shuffles the positive sample array
- Randomly shuffles the negative sample array
- Preserves all original examples (no addition or removal)
- Creates a new JSON file: `{original}_shuffled.json`

### Usage
```bash
# Using ExperimentUtilities directly
java -cp .:commons-math3-3.6.1.jar ExperimentUtilities \
     test_examples/F1_last_2_is_a_ab_dfa_queries.json \
     test_examples/F1_last_2_is_a_ab_dfa_queries_shuffled.json
```

### Expected Result
- Learning should produce the same M2MA
- Order should not affect the result
- If it fails, the algorithm may be sensitive to example order

---

## Variation 2: Redundant Examples

### Purpose
Test robustness with redundant data. Adding more examples (while maintaining language correctness) should not hurt learning.

### Implementation
- Generates random strings of specified length range
- Tests each string against the original DFA
- Adds to appropriate set (positive/negative) based on DFA acceptance
- Skips duplicates (words already in the set)
- Creates a new JSON file: `{original}_redundant.json`

### Parameters
- `numAdditional`: Number of additional examples to generate
- `minLength`: Minimum length for new examples
- `maxLength`: Maximum length for new examples

### Usage
```bash
# Using ExperimentUtilities directly
java -cp .:commons-math3-3.6.1.jar ExperimentUtilities \
     test_examples/F1_last_2_is_a_ab_dfa_queries.json \
     test_examples/F1_last_2_is_a_ab_dfa_queries_redundant.json \
     test_examples/F1_last_2_is_a_ab_dfa.dot \
     50 3 8
# Adds 50 examples, length 3-8
```

### Expected Result
- Learning should still produce equivalent M2MA
- Redundancy should not cause overfitting or confusion
- If it fails, the algorithm may be sensitive to redundant data

---

## Combined Variations

### Purpose
Test both variations together for maximum robustness.

### Implementation
1. First shuffles the samples
2. Then adds redundant examples to the shuffled file
3. Uses the final file for Phase 2 learning

### Usage

#### Using the Experiment Program
```bash
# Base experiment (no variations)
java -cp .:commons-math3-3.6.1.jar DFAToPassiveLearningExperiment \
     test_examples/F1_last_2_is_a_ab_dfa.dot 25 1000

# With variations
java -cp .:commons-math3-3.6.1.jar DFAToPassiveLearningExperiment \
     test_examples/F1_last_2_is_a_ab_dfa.dot 25 1000 true 50 3 8
# Parameters: applyVariations=true, numAdditional=50, minLength=3, maxLength=8
```

#### Using the Script
```bash
# Base experiment
bash run_experiment.sh test_examples/F1_last_2_is_a_ab_dfa.dot

# With variations
bash run_experiment.sh test_examples/F1_last_2_is_a_ab_dfa.dot 25 1000 true 50 3 8
```

### File Flow
```
Phase 1 Output:
  F1_last_2_is_a_ab_dfa_queries.json

Variation 1 (Shuffle):
  F1_last_2_is_a_ab_dfa_queries_shuffled.json

Variation 2 (Add Redundancy):
  F1_last_2_is_a_ab_dfa_queries_shuffled_redundant.json
  ↑ Used for Phase 2
```

---

## Experiment Workflow

### Step 1: Run Base Experiment
```bash
bash run_experiment.sh test_examples/F1_last_2_is_a_ab_dfa.dot
```

**Check**: Does Phase 2 learn equivalent M2MA?
- ✅ Yes → Proceed to Step 2
- ❌ No → Hypothesis disproven, investigate why

### Step 2: Run with Variations
```bash
bash run_experiment.sh test_experiments/F1_last_2_is_a_ab_dfa.dot 25 1000 true 50 3 8
```

**Check**: Does Phase 2 still learn equivalent M2MA?
- ✅ Yes → Hypothesis proven robust
- ❌ No → Variations reveal limitations

### Step 3: Analyze Results
Compare:
- Phase 1 dimension vs Phase 2 dimension
- Equivalence on collected queries
- Equivalence on random test words
- Any mismatches or differences

---

## Interpretation

### Success Criteria

**Base Experiment Success**:
- Phase 1 and Phase 2 learn same dimension
- 100% equivalence on test words
- **Conclusion**: Collected queries form a characteristic set

**Variations Success**:
- Same results as base experiment
- Order doesn't matter
- Redundancy doesn't hurt
- **Conclusion**: Learning is robust to these variations

### Failure Modes

**Base Experiment Fails**:
- Different dimensions
- Mismatches on test words
- **Possible Causes**:
  - Queries are insufficient (not a characteristic set)
  - EQ implementation issues
  - Learning algorithm limitations

**Variations Fail (Base Succeeds)**:
- Order sensitivity
- Redundancy sensitivity
- **Possible Causes**:
  - Algorithm implementation details
  - Example processing order matters
  - Overfitting to redundant data

---

## Example Output

### Base Experiment
```
PHASE 1: Learning with CharacteristicSetGeneratingOracle
  Learned dimension: 4
  Queries collected: 127

PHASE 2: Learning with PassiveLearningOracle
  Learned dimension: 4
  Examples used: 127

COMPARISON
  ✓ Dimensions match: 4
  ✓ All queries match!
  ✓ Perfect equivalence with original DFA!
```

### With Variations
```
APPLYING EXPERIMENT VARIATIONS
  Shuffled 64 positive and 63 negative examples
  Added 25 positive and 25 negative examples
  Total: 89 positive, 88 negative

PHASE 2: Learning with PassiveLearningOracle
  Learned dimension: 4
  Examples used: 177
  (Variations applied: shuffled + redundant examples)

COMPARISON
  ✓ Dimensions match: 4
  ✓ All queries match!
  ✓ Perfect equivalence with original DFA!
```

---

## Notes

- **Word Format**: All words use space-separated format (e.g., "a b c")
- **DFA Testing**: Redundant examples are tested against the original DFA to ensure language correctness
- **No Duplicates**: The system skips words that already exist in the sets
- **Deterministic**: Shuffling uses a random seed, but results are deterministic within a run
- **Language Preservation**: All variations maintain the same language (same DFA acceptance)

