# Testing Infrastructure

## Overview

The codebase has a testing infrastructure pattern used by experiment files like:
- `M2MA_experiments.java` - Tests M2MA learning on random M2MAs
- `NBA_experiments.java` - Tests NBA learning on multiple input files
- `SUBA_experiments.java` - Tests SUBA learning on multiple input files

## Pattern

The experiment files follow this pattern:
1. Read input parameters (from file or stdin)
2. Set M2MA flags (observationTableFlag, minProgressFlag, etc.)
3. Loop through test cases
4. Run learning algorithm
5. Collect statistics (runtime, dimensions, etc.)
6. Print results

## New Test Files

### OracleTests.java
Simple test program for the new oracles:
- Tests PassiveLearningOracle MQ functionality
- Tests CharacteristicSetGeneratingOracle MQ functionality
- Verifies basic oracle operations

**Usage:**
```bash
cd Code/src
javac -cp ../commons-math3-3.6.1.jar:.. OracleTests.java
java -cp ../commons-math3-3.6.1.jar:.. OracleTests
```

### Test Examples Directory

Located in `Code/test_examples/`:
- **DFA files** (`.dot`): For CharacteristicSetGeneratingOracle
- **JSON files**: For PassiveLearningOracle
- **README.md**: Documentation of test files

## Test Examples

### F1_last_2_is_a_ab
- **Language**: Words where 2nd character from end is 'a'
- **DFA**: 4 states, start state = 2
- **Examples**: 18 positive, 21 negative

### F2_xor_pos_1_2
- **Language**: Words where XOR of last two positions = 0
- **DFA**: 4 states, start state = 0
- **Examples**: 20 positive, 19 negative

## Running Tests

### Quick Test (OracleTests.java)
```bash
cd Code/src
javac -cp ../commons-math3-3.6.1.jar:.. OracleTests.java
java -cp ../commons-math3-3.6.1.jar:.. OracleTests
```

### Full Learning Test - PassiveLearningOracle
```bash
cd Code
java -cp .:commons-math3-3.6.1.jar LearnFromPassiveOracle test_examples/F1_last_2_is_a_ab.json true
```

### Full Learning Test - CharacteristicSetGeneratingOracle
```bash
cd Code
java -cp .:commons-math3-3.6.1.jar LearnFromDFA test_examples/F1_last_2_is_a_ab_dfa.dot 25 1000
```

## Future Testing

To create more comprehensive tests following the experiment pattern:

1. **OracleExperiments.java** - Similar to NBA_experiments.java
   - Test multiple DFA/JSON files
   - Collect statistics (runtime, queries, dimensions)
   - Compare results

2. **Integration Tests**
   - Test oracle integration with M2MA
   - Verify EQ counterexamples work correctly
   - Test edge cases (empty examples, single state DFA, etc.)

3. **Regression Tests**
   - Test against known good results
   - Verify oracle behavior matches expected output

## Notes

- Test examples are adapted from `~/dfa-experiment/generated_characteristic_sets/`
- JSON format uses "Positive sample" and "Negative sample" keys (not "positive"/"negative")
- DOT files are simplified (no `__start` node, first state is start state)





