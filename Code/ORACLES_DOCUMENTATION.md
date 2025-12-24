# Oracle Documentation

This document describes the two new oracles added to the ALMA learning system: `PassiveLearningOracle` and `CharacteristicSetGeneratingOracle`.

## Overview

Both oracles are integrated into `M2MA.java` and follow the existing oracle pattern used by `NBA.java` and `arbitrary.java`. They use an `active` flag for detection (unlike NBA/arbitrary which use field presence checks).

## 1. PassiveLearningOracle

### Purpose
Passive learning oracle that learns from a fixed set of examples. Unlike active learning, this oracle only uses the provided examples and does not generate new queries.

### Features
- Loads examples from JSON files
- Answers membership queries based on loaded examples
- Performs equivalence queries by checking all examples
- Supports closed-world assumption (unknown words can be treated as negative)

### API

#### Loading Examples
```java
String[] alphabet = PassiveLearningOracle.loadFromJSON("examples.json");
```
- Loads positive and negative examples from JSON file
- Returns the alphabet extracted from the JSON
- Expected JSON format:
  ```json
  {
    "metadata": { "alphabet": ["a", "b"], ... },
    "Positive sample": ["", "a a", ...],
    "Negative sample": ["a", "b", ...]
  }
  ```

#### Membership Query
```java
int result = PassiveLearningOracle.MQ("a b");
```
- Returns `1` if word is in positive examples
- Returns `0` if word is in negative examples
- If `closedWorld = true`: returns `0` for unknown words
- If `closedWorld = false`: throws `RuntimeException` for unknown words

#### Equivalence Query
```java
boolean isCorrect = PassiveLearningOracle.EQ(hypothesisFinalVector, hypothesisTransitionMatrices);
```
- Checks that all positive words are accepted (return 1)
- Checks that all negative words are rejected (return 0)
- Returns `false` on first mismatch (sets `M2MA.counterExample`)
- Returns `true` if all examples match

#### Configuration
```java
PassiveLearningOracle.active = true;  // Enable oracle
PassiveLearningOracle.closedWorld = false;  // Throw exception for unknown words
```

### Usage Example
```java
// Load examples
String[] alphabet = PassiveLearningOracle.loadFromJSON("examples.json");

// Activate oracle
PassiveLearningOracle.active = true;
PassiveLearningOracle.closedWorld = true;

// Set up alphabet in M2MA
M2MA.alphabet = alphabet;
M2MA.letterToIndex = new HashMap<>();
for (int i = 0; i < alphabet.length; i++) {
    M2MA.letterToIndex.put(alphabet[i], i);
}

// Run learning
M2MA.learn();
```

---

## 2. CharacteristicSetGeneratingOracle

### Purpose
Oracle that loads a DFA from a `.dot` file and answers membership queries. Also collects all membership queries and their answers for analysis.

### Features
- Loads DFA from Graphviz DOT format files
- Answers membership queries by simulating the DFA
- Collects all queries and answers for statistics
- Uses statistical equivalence queries (random test generation)

### API

#### Loading DFA
```java
CharacteristicSetGeneratingOracle.loadDFAFromDot("dfa.dot");
```
- Parses Graphviz DOT format
- Extracts states, transitions, accepting states, and alphabet
- Expected DOT format:
  ```
  digraph {
    q0 [shape=doublecircle];  // accepting state
    q1 [shape=circle];         // non-accepting state
    q0 -> q1 [label="a"];
    q1 -> q0 [label="b"];
  }
  ```

#### Membership Query
```java
int result = CharacteristicSetGeneratingOracle.MQ("a b");
```
- Simulates the DFA on the input word
- Returns `1` if accepted, `0` if rejected
- Automatically collects query and answer in `allQueries` map
- Uses Hankel cache for efficiency

#### Equivalence Query
```java
boolean isCorrect = CharacteristicSetGeneratingOracle.EQ(hypothesisFinalVector, hypothesisTransitionMatrices);
```
- Uses statistical testing with random strings
- Tests up to `EQNumTests` random strings (default: 1000)
- Maximum test length: `EQMaxTestLen` (default: 25)
- Returns `false` on first counterexample found
- Returns `true` if all tests pass

#### Query Statistics
```java
CharacteristicSetGeneratingOracle.printQueryStatistics();
// Prints: Total queries, unique queries, positive/negative counts

Set<String> positiveWords = new HashSet<>();
Set<String> negativeWords = new HashSet<>();
CharacteristicSetGeneratingOracle.getCollectedQueries(positiveWords, negativeWords);
```

#### Configuration
```java
CharacteristicSetGeneratingOracle.active = true;  // Enable oracle
CharacteristicSetGeneratingOracle.setEQParameters(25, 1000);  // maxTestLen, numTests
```

### Usage Example
```java
// Load DFA
CharacteristicSetGeneratingOracle.loadDFAFromDot("dfa.dot");

// Configure EQ parameters (optional)
CharacteristicSetGeneratingOracle.setEQParameters(30, 2000);

// Activate oracle
CharacteristicSetGeneratingOracle.active = true;

// Set up alphabet in M2MA
M2MA.alphabet = CharacteristicSetGeneratingOracle.alphabet;
M2MA.letterToIndex = new HashMap<>();
for (int i = 0; i < M2MA.alphabet.length; i++) {
    M2MA.letterToIndex.put(M2MA.alphabet[i], i);
}

// Run learning
M2MA.learn();

// Print statistics
CharacteristicSetGeneratingOracle.printQueryStatistics();
```

---

## Integration with M2MA

Both oracles are automatically detected by `M2MA.java`:

### MQ Routing (in `M2MA.MQ()`)
```java
if (PassiveLearningOracle.active) {
    out = PassiveLearningOracle.MQ(word);
}
else if (CharacteristicSetGeneratingOracle.active) {
    out = CharacteristicSetGeneratingOracle.MQ(word);
}
// ... other oracles
```

### EQ Routing (in `M2MA.EQ()`)
```java
if (PassiveLearningOracle.active) {
    return PassiveLearningOracle.EQ(hypothesisFinalVector, hypothesisTransitionMatrices);
}
if (CharacteristicSetGeneratingOracle.active) {
    return CharacteristicSetGeneratingOracle.EQ(hypothesisFinalVector, hypothesisTransitionMatrices);
}
// ... other oracles
```

**Note:** Only one oracle should be active at a time. The priority order is:
1. PassiveLearningOracle
2. CharacteristicSetGeneratingOracle
3. NBA (if `NBAFinalStates != null`)
4. arbitrary (if `MQMethod != null`)
5. Default (uses minimized M2MA)

---

## Command-Line Usage

Wrapper programs are provided for command-line usage, following the pattern of `NBA.java` and `arbitrary.java`:

### LearnFromPassiveOracle.java

Wrapper for `PassiveLearningOracle` that loads examples from JSON and runs learning.

**Usage:**
```bash
java LearnFromPassiveOracle <input.json> [closedWorld]
```

**Arguments:**
- `input.json` - JSON file with positive and negative examples
- `closedWorld` - (optional) "true" or "false" - treat unknown words as negative (default: false)

**Example:**
```bash
java LearnFromPassiveOracle examples.json true
```

### LearnFromDFA.java

Wrapper for `CharacteristicSetGeneratingOracle` that loads a DFA from a DOT file and runs learning.

**Usage:**
```bash
java LearnFromDFA <input.dot> [maxTestLen] [numTests]
```

**Arguments:**
- `input.dot` - Graphviz DOT file with DFA definition
- `maxTestLen` - (optional) Maximum length for EQ test strings (default: 25)
- `numTests` - (optional) Number of EQ test strings (default: 1000)

**Example:**
```bash
java LearnFromDFA dfa.dot 30 2000
```

---

## Differences from Existing Oracles

### Pattern Comparison

| Feature | NBA/arbitrary | New Oracles |
|---------|---------------|-------------|
| Detection | Field presence (`NBAFinalStates != null`) | Active flag (`active = true`) |
| EQ Implementation | Shared (`arbitrary.EQstatistical`) | Individual methods |
| Initialization | In `readInput()` | Separate `load*()` methods |
| Configuration | File-based | Programmatic + file-based |

### Why Different Patterns?

The new oracles use explicit `active` flags for:
- **Clarity**: Explicit activation vs. implicit field presence
- **Flexibility**: Can be activated/deactivated programmatically
- **Safety**: Won't accidentally activate if fields are set but not intended

---

## Files

- `PassiveLearningOracle.java` - Passive learning oracle implementation
- `CharacteristicSetGeneratingOracle.java` - DFA-based oracle implementation
- `M2MA.java` - Updated with oracle integration (lines 797-803, 851-857)
- `ExampleLoader.java` - Shared JSON loading code (used by PassiveLearningOracle)

---

## Example Workflows

### Passive Learning Workflow
1. Prepare JSON file with examples
2. Load examples: `PassiveLearningOracle.loadFromJSON("examples.json")`
3. Activate: `PassiveLearningOracle.active = true`
4. Configure: `PassiveLearningOracle.closedWorld = true/false`
5. Set up M2MA alphabet
6. Run: `M2MA.learn()`

### DFA Learning Workflow
1. Prepare DOT file with DFA
2. Load DFA: `CharacteristicSetGeneratingOracle.loadDFAFromDot("dfa.dot")`
3. Configure EQ: `CharacteristicSetGeneratingOracle.setEQParameters(25, 1000)`
4. Activate: `CharacteristicSetGeneratingOracle.active = true`
5. Set up M2MA alphabet
6. Run: `M2MA.learn()`
7. Analyze: `CharacteristicSetGeneratingOracle.printQueryStatistics()`

