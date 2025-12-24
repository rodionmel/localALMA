# DFA Learning Experiment Script

## Overview

The `run_dfa_experiment.sh` script provides a complete pipeline for running the DFA learning experiment with automatic variation generation and testing.

## Usage

```bash
./run_dfa_experiment.sh <dfa.dot> [maxTestLen] [numAdditional] [minLength] [maxLength]
```

### Arguments

- **dfa.dot** (required): Path to Graphviz DOT file with DFA definition
- **maxTestLen** (optional, default: 10): Maximum length for EQ test strings
- **numAdditional** (optional, default: 50): Number of redundant examples to add
- **minLength** (optional, default: 3): Minimum length for redundant examples
- **maxLength** (optional, default: 8): Maximum length for redundant examples

### Example

```bash
# From the Code directory
./run_dfa_experiment.sh test_examples/F1_last_2_is_a_ab_dfa.dot

# With custom parameters
./run_dfa_experiment.sh test_examples/F1_last_2_is_a_ab_dfa.dot 10 50 3 8

# From another directory (using absolute path)
/path/to/localALMA/Code/run_dfa_experiment.sh /path/to/dfa.dot
```

## What It Does

1. **Phase 1: Active Learning**
   - Loads DFA from `.dot` file
   - Runs M2MA learning with CharacteristicSetGeneratingOracle
   - Collects all queries made during learning
   - Outputs queries to JSON: `{dfa}_queries.json`

2. **Variations Generation**
   - Shuffles positive and negative samples
   - Adds redundant examples (random words tested against DFA)
   - Creates: `{dfa}_queries_shuffled_redundant.json`

3. **Phase 2: Passive Learning**
   - Loads shuffled + redundant examples
   - Runs M2MA learning with PassiveLearningOracle
   - Verifies equivalence

4. **Output**
   - Comparison of dimensions
   - Verification against original DFA
   - Log comparison (MQ calls and counterexamples)
   - Generated JSON files

## Generated Files

For a DFA file `example.dfa.dot`, the script generates:

1. **`example.dfa_queries.json`**: Base queries from Phase 1
2. **`example.dfa_queries_shuffled.json`**: Shuffled samples
3. **`example.dfa_queries_shuffled_redundant.json`**: Shuffled + redundant examples (used in Phase 2)

## Requirements

- Java 11 (loaded via `module load Java/11.0.16`)
- Apache Commons Math 3.6.1 (`commons-math3-3.6.1.jar`)
- All source files compiled in the Code directory

## Running from Other Directories

The script uses absolute paths, so it can be run from anywhere:

```bash
# From any directory
/path/to/localALMA/Code/run_dfa_experiment.sh /path/to/your/dfa.dot
```

The script will:
- Automatically find its own directory
- Compile if needed
- Run the experiment
- Generate output files in the same directory as the DFA file (or Code directory)

## Output Interpretation

### Success Indicators

- ✓ Dimensions match: Phase 1 and Phase 2 learn same dimension
- ✓ All queries match: Phase 2 correctly classifies all Phase 1 queries
- ✓ Perfect equivalence: Phase 2 M2MA matches original DFA (100/100 random words)
- ✓ Counterexamples match: Both phases find same counterexamples

### Failure Indicators

- ✗ Dimensions differ: Different M2MAs learned
- ✗ Mismatches found: Phase 2 doesn't match original DFA
- ✗ Different counterexamples: Learning paths diverged

## Notes

- The script automatically compiles Java files if needed
- It loads Java 11 via module system
- All paths are handled automatically (relative or absolute)
- Output files are created in the same directory as the input DFA file

