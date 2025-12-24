# DFA Learning Experiment Script - Summary

## Created Script

**File**: `run_dfa_experiment.sh`

A standalone bash script that encapsulates the complete experiment pipeline and can be run from any directory.

## Features

1. **Automatic Path Resolution**: Handles relative and absolute paths for DFA files
2. **Auto-compilation**: Compiles Java files if needed
3. **Complete Pipeline**: Runs full experiment with variations
4. **Portable**: Can be run from any directory

## Usage

```bash
# From Code directory
./run_dfa_experiment.sh test_examples/F1_last_2_is_a_ab_dfa.dot

# From another directory (relative path)
/path/to/localALMA/Code/run_dfa_experiment.sh Code/test_examples/F1_last_2_is_a_ab_dfa.dot

# From another directory (absolute path)
/path/to/localALMA/Code/run_dfa_experiment.sh /path/to/dfa.dot

# With custom parameters
./run_dfa_experiment.sh test_examples/F1_last_2_is_a_ab_dfa.dot 10 50 3 8
```

## What It Does

### 1. Phase 1: Active Learning (CharacteristicSetGeneratingOracle)
- Loads DFA from `.dot` file
- Runs M2MA learning
- Collects all queries
- Outputs: `{dfa}_queries.json`

### 2. Variations Generation
- **Shuffling**: Randomly reorders positive/negative samples
  - Output: `{dfa}_queries_shuffled.json`
- **Redundant Examples**: Adds random words (tested against DFA)
  - Output: `{dfa}_queries_shuffled_redundant.json`

### 3. Phase 2: Passive Learning (PassiveLearningOracle)
- Loads shuffled + redundant examples
- Runs M2MA learning
- Verifies equivalence

### 4. Output
- Comparison results
- Verification against original DFA
- Log comparison (MQ calls and counterexamples)
- Generated JSON files

## Generated Files

For input `example.dfa.dot`:

1. **`example.dfa_queries.json`**: Base queries from Phase 1
2. **`example.dfa_queries_shuffled.json`**: Shuffled samples
3. **`example.dfa_queries_shuffled_redundant.json`**: Final examples used in Phase 2

## Test Results

✅ **Script tested successfully**:
- Works from different directories
- Handles relative and absolute paths
- Auto-compiles when needed
- Generates all output files
- Shows complete results

## Example Output

```
==========================================
DFA Learning Experiment
==========================================

Configuration:
  DFA file: /path/to/dfa.dot
  Max test length: 10
  Additional examples: 50
  Length range: 3-8

Phase 1: Learned dimension 3, 15 queries collected
Phase 2: Learned dimension 3, 65 examples used
✓ Dimensions match
✓ Perfect equivalence with original DFA
✓ All counterexamples match

Generated files:
  Base queries: dfa_queries.json
  Shuffled: dfa_queries_shuffled.json
  With redundancy: dfa_queries_shuffled_redundant.json
```

## Requirements

- Java 11 (loaded via `module load Java/11.0.16`)
- Apache Commons Math 3.6.1 (`commons-math3-3.6.1.jar` in Code directory)
- All source files in `Code/src/`

## Notes

- Script automatically finds its directory
- Changes to script directory for execution
- Output files created in same directory as input DFA file
- Handles compilation automatically
- Provides clear error messages if files not found

