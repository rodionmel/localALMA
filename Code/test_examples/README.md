# Test Examples

This directory contains small test examples for the new oracles.

## Files

### DFA Examples (for CharacteristicSetGeneratingOracle)

1. **F1_last_2_is_a_ab_dfa.dot**
   - Language: Words where the 2nd character from the end is 'a'
   - States: 4 states (0, 1, 2, 3)
   - Accepting states: 1, 3
   - Alphabet: {a, b}
   - Example positive words: "aa", "ab", "baa", "bab"
   - Example negative words: "a", "b", "aba", "abb"

2. **F2_xor_pos_1_2_dfa.dot**
   - Language: Words where XOR of last two positions equals 0
   - States: 4 states (0, 1, 2, 3)
   - Accepting states: 0, 2
   - Alphabet: {a, b}
   - Example positive words: "", "a", "aa", "bb"
   - Example negative words: "b", "ab", "ba"

### JSON Examples (for PassiveLearningOracle)

1. **F1_last_2_is_a_ab.json**
   - Corresponds to F1_last_2_is_a_ab_dfa.dot
   - 18 positive examples
   - 21 negative examples
   - Alphabet: ["a", "b"]

2. **F2_xor_pos_1_2.json**
   - Corresponds to F2_xor_pos_1_2_dfa.dot
   - 20 positive examples
   - 19 negative examples
   - Alphabet: ["a", "b"]

## Usage

### Test PassiveLearningOracle
```bash
cd Code
java -cp .:commons-math3-3.6.1.jar LearnFromPassiveOracle test_examples/F1_last_2_is_a_ab.json true
```

### Test CharacteristicSetGeneratingOracle
```bash
cd Code
java -cp .:commons-math3-3.6.1.jar LearnFromDFA test_examples/F1_last_2_is_a_ab_dfa.dot 25 1000
```

### Run Simple Tests
```bash
cd Code/src
javac -cp ../commons-math3-3.6.1.jar:.. OracleTests.java
java -cp ../commons-math3-3.6.1.jar:.. OracleTests
```

## Source

These examples were copied and adapted from:
`~/dfa-experiment/generated_characteristic_sets/`

The JSON files were adapted to use "Positive sample" and "Negative sample" keys (instead of "positive" and "negative") to match the PassiveLearningOracle format.

The DOT files were simplified by removing the `__start` node and related syntax that our parser doesn't handle.





