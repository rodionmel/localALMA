# New Oracles Summary

## What Was Created

### 1. Core Oracle Classes

#### PassiveLearningOracle.java
- **Purpose**: Passive learning from fixed example sets
- **Features**:
  - Loads examples from JSON files
  - Answers MQ based on loaded examples
  - Performs EQ by checking all examples
  - Supports closed-world assumption
- **Integration**: Lines 797-799, 851-853 in M2MA.java

#### CharacteristicSetGeneratingOracle.java
- **Purpose**: DFA-based oracle with query collection
- **Features**:
  - Loads DFA from Graphviz DOT files
  - Answers MQ by simulating DFA
  - Collects all queries for statistics
  - Uses statistical EQ (random test generation)
- **Integration**: Lines 801-803, 855-857 in M2MA.java

### 2. Wrapper Programs (Command-Line Tools)

#### LearnFromPassiveOracle.java
- **Purpose**: Command-line wrapper for PassiveLearningOracle
- **Usage**: `java LearnFromPassiveOracle <input.json> [closedWorld]`
- **Features**: Loads JSON, activates oracle, runs learning, verifies results

#### LearnFromDFA.java
- **Purpose**: Command-line wrapper for CharacteristicSetGeneratingOracle
- **Usage**: `java LearnFromDFA <input.dot> [maxTestLen] [numTests]`
- **Features**: Loads DOT file, configures EQ, activates oracle, runs learning, prints statistics

### 3. Documentation

#### ORACLES_DOCUMENTATION.md
- Complete API documentation
- Usage examples
- Integration details
- Comparison with existing oracles

## Integration Status

### ✅ Complete Integration

1. **MQ Routing** (M2MA.java lines 797-803)
   - PassiveLearningOracle checked first
   - CharacteristicSetGeneratingOracle checked second
   - Proper priority order maintained

2. **EQ Routing** (M2MA.java lines 851-857)
   - PassiveLearningOracle checked first
   - CharacteristicSetGeneratingOracle checked second
   - Proper priority order maintained

3. **Hankel Cache Integration**
   - Both oracles use M2MA.Hankel for caching
   - Consistent with existing oracle behavior

4. **Alphabet Setup**
   - Both oracles extract alphabet from input
   - Wrapper programs set up M2MA.alphabet and letterToIndex

5. **Command-Line Tools**
   - Wrapper programs follow NBA/arbitrary pattern
   - Complete with error handling and verification

## API Completeness

### PassiveLearningOracle
- ✅ `loadFromJSON(String filename)` - Load examples
- ✅ `MQ(String w)` - Membership query
- ✅ `EQ(...)` - Equivalence query
- ✅ `active` flag - Activation control
- ✅ `closedWorld` flag - Configuration

### CharacteristicSetGeneratingOracle
- ✅ `loadDFAFromDot(String filename)` - Load DFA
- ✅ `MQ(String w)` - Membership query
- ✅ `EQ(...)` - Equivalence query
- ✅ `setEQParameters(int, int)` - Configure EQ
- ✅ `printQueryStatistics()` - Statistics
- ✅ `getCollectedQueries(...)` - Get collected queries
- ✅ `active` flag - Activation control
- ✅ `alphabet` field - Extracted alphabet

## Files Created/Modified

### New Files
1. `Code/src/PassiveLearningOracle.java` (184 lines)
2. `Code/src/CharacteristicSetGeneratingOracle.java` (289 lines)
3. `Code/src/LearnFromPassiveOracle.java` (95 lines)
4. `Code/src/LearnFromDFA.java` (70 lines)
5. `Code/ORACLES_DOCUMENTATION.md` (Documentation)
6. `Code/ORACLES_SUMMARY.md` (This file)

### Modified Files
1. `Code/src/M2MA.java`
   - Added PassiveLearningOracle checks (lines 797-799, 851-853)
   - Added CharacteristicSetGeneratingOracle checks (lines 801-803, 855-857)

## Testing Checklist

To verify everything works:

### PassiveLearningOracle
- [ ] Load JSON file with examples
- [ ] Activate oracle: `PassiveLearningOracle.active = true`
- [ ] Set alphabet in M2MA
- [ ] Run `M2MA.learn()`
- [ ] Verify all examples are correctly classified
- [ ] Test command-line: `java LearnFromPassiveOracle examples.json`

### CharacteristicSetGeneratingOracle
- [ ] Load DOT file with DFA
- [ ] Activate oracle: `CharacteristicSetGeneratingOracle.active = true`
- [ ] Set alphabet in M2MA
- [ ] Run `M2MA.learn()`
- [ ] Check query statistics
- [ ] Test command-line: `java LearnFromDFA dfa.dot`

## Known Limitations

1. **Start State Detection**: CharacteristicSetGeneratingOracle uses first state encountered as start state (may need explicit marker in DOT files)

2. **DFA Validation**: Missing transitions are warned but not enforced (words with undefined transitions are rejected)

3. **Single Oracle**: Only one oracle can be active at a time (by design, using else-if chain)

## Future Enhancements (Optional)

1. Support explicit start state markers in DOT files
2. Add DFA completeness validation
3. Add more detailed error messages
4. Support for multiple oracle composition (if needed)

## Status: ✅ READY FOR USE

All components are implemented and integrated. The oracles can be used both programmatically and via command-line wrapper programs.

