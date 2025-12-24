# Experiment Plan: DFA → CharacteristicSetGeneratingOracle → PassiveLearningOracle → M2MA

## Hypothesis

If we:
1. Load a DFA into CharacteristicSetGeneratingOracle
2. Run M2MA learning (which queries the DFA oracle)
3. Collect all queries made during learning
4. Use those queries as examples for PassiveLearningOracle
5. Run M2MA learning again with PassiveLearningOracle

Then the learned M2MA should be equivalent to the original DFA.

## High-Level Plan

### Step 1: Create Experiment Runner Program
**Action**: Create `DFAToPassiveLearningExperiment.java`

**Purpose**: Orchestrates the full experiment:
- Loads DFA into CharacteristicSetGeneratingOracle
- Runs learning and collects queries
- Outputs queries to JSON
- Switches to PassiveLearningOracle
- Runs learning again
- Compares results

**Key Features**:
- Takes DFA file as input
- Configures EQ parameters
- Captures all queries during first learning phase
- Automatically switches oracles
- Verifies equivalence

### Step 2: Modify LearnFromDFA to Output Queries
**Action**: Update `LearnFromDFA.java` to output queries after learning

**Changes**:
- After `M2MA.learn()` completes
- Call `CharacteristicSetGeneratingOracle.outputQueriesToJSON(outputFile)`
- Output file: `{input_dot_file}_queries.json`

### Step 3: Create Verification Program
**Action**: Create `VerifyEquivalence.java`

**Purpose**: Verifies that learned M2MA is equivalent to original DFA

**Functionality**:
- Loads original DFA (CharacteristicSetGeneratingOracle)
- Loads learned M2MA (from PassiveLearningOracle run)
- Tests on a set of words:
  - All words from collected queries
  - Additional random test words
- Compares DFA answer vs M2MA answer
- Reports equivalence or differences

### Step 4: Create Test Script
**Action**: Create `run_experiment.sh`

**Purpose**: Automates the full experiment workflow

**Script Flow**:
1. Compile all necessary classes
2. Run Step 1: Learn with CharacteristicSetGeneratingOracle
   - Input: `test_examples/F1_last_2_is_a_ab_dfa.dot`
   - Output: `test_examples/F1_last_2_is_a_ab_dfa_queries.json`
3. Run Step 2: Learn with PassiveLearningOracle
   - Input: `test_examples/F1_last_2_is_a_ab_dfa_queries.json`
   - Output: Learned M2MA
4. Run Step 3: Verify equivalence
   - Compare DFA vs learned M2MA
5. Report results

### Step 5: Create Analysis Program
**Action**: Create `AnalyzeQueries.java`

**Purpose**: Analyzes the collected queries

**Analysis**:
- Number of queries made
- Distribution of query lengths
- Coverage of the language
- Whether queries form a characteristic set
- Compare with original example set (if available)

## Detailed Implementation Plan

### File 1: `DFAToPassiveLearningExperiment.java`

**Structure**:
```java
public class DFAToPassiveLearningExperiment {
    public static void main(String[] args) {
        // 1. Load DFA
        CharacteristicSetGeneratingOracle.loadDFAFromDot(args[0]);
        
        // 2. Phase 1: Learn with CharacteristicSetGeneratingOracle
        //    - Activate oracle
        //    - Run M2MA.learn()
        //    - Collect queries
        
        // 3. Output queries to JSON
        String queriesFile = args[0].replace(".dot", "_queries.json");
        CharacteristicSetGeneratingOracle.outputQueriesToJSON(queriesFile);
        
        // 4. Optional: Apply variations (shuffling + redundancy)
        //    - Shuffle samples
        //    - Add redundant examples
        
        // 5. Phase 2: Learn with PassiveLearningOracle
        //    - Load queries JSON (possibly modified)
        //    - Activate PassiveLearningOracle
        //    - Run M2MA.learn() again
        
        // 6. Compare results
        //    - Save both M2MAs
        //    - Run verification
    }
}
```

**Usage**: 
- Base experiment: `java DFAToPassiveLearningExperiment <dfa.dot> [maxTestLen] [numTests]`
- With variations: `java DFAToPassiveLearningExperiment <dfa.dot> [maxTestLen] [numTests] true [numAdditional] [minLength] [maxLength]`

### File 1b: `ExperimentUtilities.java`

**Purpose**: Utility functions for experiment variations

**Functions**:
- `shuffleSamples(inputJson, outputJson)`: Randomly reorders positive and negative samples
- `addRedundantExamples(inputJson, outputJson, dfaFile, numAdditional, minLength, maxLength)`: 
  - Generates random strings
  - Tests them against DFA
  - Adds to appropriate set (positive/negative)
  - Maintains language correctness

### File 2: `VerifyEquivalence.java`

**Structure**:
```java
public class VerifyEquivalence {
    public static void main(String[] args) {
        // 1. Load original DFA
        CharacteristicSetGeneratingOracle.loadDFAFromDot(args[0]);
        
        // 2. Load learned M2MA (from PassiveLearningOracle run)
        //    - Read M2MA.resultFinalVector
        //    - Read M2MA.resultTransitionMatrices
        
        // 3. Test equivalence
        //    - Test all words from queries
        //    - Test additional random words
        //    - Compare DFA.MQ(word) vs M2MA.MQArbitrary(word)
        
        // 4. Report results
    }
}
```

**Usage**: `java VerifyEquivalence <dfa.dot>`

### File 3: `run_experiment.sh`

**Script Content**:
```bash
#!/bin/bash
# Load Java 11
module load Java/11.0.16

# Compile
javac -cp .:commons-math3-3.6.1.jar src/*.java -d .

# Run experiment
DFA_FILE="test_examples/F1_last_2_is_a_ab_dfa.dot"
java -cp .:commons-math3-3.6.1.jar DFAToPassiveLearningExperiment $DFA_FILE 25 1000

# Verify
java -cp .:commons-math3-3.6.1.jar VerifyEquivalence $DFA_FILE
```

**Usage**: `bash run_experiment.sh`

## Expected Outcomes

### Success Case
- All queries from Phase 1 are sufficient
- Phase 2 learns equivalent M2MA
- Verification shows 100% equivalence
- **Conclusion**: Collected queries form a characteristic set

### Failure Case
- Phase 2 learns different M2MA
- Verification shows differences
- **Analysis Needed**: 
  - Are queries sufficient?
  - Is EQ implementation correct?
  - Are there missing distinguishing strings?

## Metrics to Collect

1. **Query Statistics**:
   - Total queries in Phase 1
   - Unique queries
   - Positive vs negative distribution
   - Query length distribution

2. **Learning Statistics**:
   - Phase 1: Learned M2MA dimension
   - Phase 2: Learned M2MA dimension
   - Comparison: Are dimensions equal?

3. **Equivalence Statistics**:
   - Number of test words
   - Number of mismatches
   - Percentage equivalence

4. **Performance**:
   - Phase 1 runtime
   - Phase 2 runtime
   - Total runtime

## Test Cases

### Test Case 1: Small DFA (Base Experiment)
- **Input**: `F1_last_2_is_a_ab_dfa.dot` (4 states)
- **Expected**: Should work if hypothesis is true
- **Command**: `java DFAToPassiveLearningExperiment test_examples/F1_last_2_is_a_ab_dfa.dot`

### Test Case 2: Medium DFA (Base Experiment)
- **Input**: `F2_xor_pos_1_2_dfa.dot` (4 states)
- **Expected**: Should work if hypothesis is true
- **Command**: `java DFAToPassiveLearningExperiment test_examples/F2_xor_pos_1_2_dfa.dot`

### Test Case 3: Harder Experiment (Variations)
- **Input**: Any DFA from test set
- **Variations Applied**:
  1. **Shuffling**: Randomly reorder positive and negative samples
  2. **Redundancy**: Add more random examples (longer strings) based on DFA acceptance
- **Command**: `java DFAToPassiveLearningExperiment <dfa.dot> 25 1000 true 50 3 8`
  - `true`: Apply variations
  - `50`: Add 50 redundant examples
  - `3-8`: Length range for redundant examples
- **Expected**: Should still work if hypothesis is robust

## Implementation Order

1. **First**: Create `DFAToPassiveLearningExperiment.java` (core experiment)
2. **Second**: Create `ExperimentUtilities.java` (variations: shuffling + redundancy)
3. **Third**: Update `LearnFromDFA.java` to output queries
4. **Fourth**: Create `VerifyEquivalence.java` (verification)
5. **Fifth**: Create `run_experiment.sh` (automation)
6. **Sixth**: Create `AnalyzeQueries.java` (analysis - optional)

## Experiment Variations

### Variation 1: Random Reordering
- **Purpose**: Test if order of examples matters
- **Implementation**: Shuffle positive and negative arrays before Phase 2
- **Expected**: Should not affect learning (order-independent)

### Variation 2: Redundant Examples
- **Purpose**: Test robustness with redundant data
- **Implementation**: 
  - Generate random strings (longer than original queries)
  - Test against DFA
  - Add to appropriate set
- **Expected**: Should still learn correctly (redundancy shouldn't hurt)
- **Parameters**:
  - Number of additional examples
  - Length range for new examples

## Success Criteria

The experiment **proves** the hypothesis if:
- ✅ Phase 2 learns M2MA with same dimension as Phase 1
- ✅ Verification shows 100% equivalence on test words
- ✅ All collected queries are correctly classified by learned M2MA

The experiment **disproves** the hypothesis if:
- ❌ Phase 2 learns different M2MA
- ❌ Verification shows mismatches
- ❌ Learned M2MA doesn't match original DFA

## Notes

- The experiment assumes that queries collected during learning are sufficient
- This tests whether the learning algorithm's queries form a characteristic set
- If it fails, we may need to collect additional queries beyond those made during learning
- The comparison is between the DFA oracle and the learned M2MA (not between Phase 1 and Phase 2 M2MAs)

