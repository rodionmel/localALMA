# Sources of Randomness in Learning Process

## Primary Source: CharacteristicSetGeneratingOracle.EQ()

**Location**: `CharacteristicSetGeneratingOracle.java`, lines 336-337

```java
for (int i=0; i<EQNumTests; i++) {
    String test = M2MA.genTest((int) (Math.random() * (EQMaxTestLen + 1)), false);
    // ... test the hypothesis ...
}
```

**What's random**:
1. **Test length**: `(int) (Math.random() * (EQMaxTestLen + 1))` - randomly chooses length from 0 to EQMaxTestLen
2. **Test generation**: `M2MA.genTest()` uses `Math.random()` to randomly select alphabet symbols

**Impact**: 
- Different random seeds produce different test strings
- Different counterexamples may be found
- Even with "shortest counterexample" strategy, which counterexample is found depends on which random tests are generated

## Secondary Source: M2MA.genTest()

**Location**: `M2MA.java`, lines 1037-1050

```java
public static String genTest(int len, boolean smallerAlphabet) {
    // ...
    for (int i=0; i<len-1; i++) {
        test += alphabet[(int) (Math.random() * length)] + " ";
    }
    if (len >= 1) {
        test += alphabet[(int) (Math.random() * length)];
    }
    return test;
}
```

**What's random**:
- Each symbol in the test string is randomly selected from the alphabet
- Uses `Math.random()` to pick array indices

**Impact**:
- Same length can produce different strings
- Affects which counterexamples are discovered

## How Randomness Affects Results

### 1. Different Counterexamples
- Each run generates different random test strings
- Different counterexamples may be found
- Even with "shortest" strategy, the set of counterexamples to choose from is random

### 2. Learning Path Variation
- Different counterexamples lead to different learning paths
- The algorithm adds counterexamples to the observation table
- Different counterexamples may lead to different final M2MAs

### 3. Query Collection Variation
- Different counterexamples mean different queries collected
- The final set of queries depends on which counterexamples were found
- This affects Phase 2 passive learning

## Example

**Run 1**:
- Random test: "a b a a b" (length 5) - finds counterexample
- Random test: "a a" (length 2) - finds counterexample
- Shortest: "a a" (length 2) - used for learning

**Run 2**:
- Random test: "b b a a b" (length 5) - finds counterexample
- Random test: "b a" (length 2) - finds counterexample  
- Shortest: "b a" (length 2) - used for learning

Both runs find length-2 counterexamples, but they're different words, leading to:
- Different queries collected
- Potentially different learning paths
- Different final M2MAs

## Why Results Vary

The variation you observed (dimension 5 vs 6, error rate 21% vs 28%) is due to:

1. **Random test generation**: Each EQ call generates different random tests
2. **Different counterexamples found**: Even shortest ones may differ
3. **Different learning paths**: Different counterexamples lead to different observation tables
4. **Different final M2MAs**: Different learning paths converge to different (but equivalent) M2MAs

## Deterministic Alternatives

To make results reproducible, you could:

1. **Set random seed**: `Random rng = new Random(seed);` and use `rng.nextInt()` instead of `Math.random()`
2. **Systematic testing**: Instead of random tests, use systematic enumeration (all words up to length N)
3. **Fixed test set**: Use a predefined set of test strings

However, randomness is actually **beneficial** for statistical EQ because:
- It provides good coverage of the language
- It's efficient (doesn't need to test all possible strings)
- It's unbiased

## Summary

**Main randomness source**: `CharacteristicSetGeneratingOracle.EQ()` generates random test strings using `Math.random()`

**Impact**: 
- Different runs produce different counterexamples
- Different learning paths
- Slightly different final results (but should be equivalent)

**This is expected behavior** for statistical equivalence queries.

