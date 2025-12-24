#!/bin/bash
# Experiment Runner Script
# Tests if queries from CharacteristicSetGeneratingOracle are sufficient
# to learn equivalent M2MA with PassiveLearningOracle

# Load Java 11
module load Java/11.0.16

# Set working directory
cd "$(dirname "$0")"

echo "=========================================="
echo "DFA to Passive Learning Experiment"
echo "=========================================="
echo ""

# Check arguments
if [ $# -lt 1 ]; then
    echo "Usage: bash run_experiment.sh <dfa.dot> [maxTestLen] [numTests] [applyVariations] [numAdditional] [minLength] [maxLength]"
    echo "  dfa.dot         : Graphviz DOT file with DFA definition"
    echo "  maxTestLen      : max length for EQ test strings (default: 25)"
    echo "  numTests        : number of EQ test strings (default: 1000)"
    echo "  applyVariations : true/false - apply shuffling and redundancy (default: false)"
    echo "  numAdditional   : number of redundant examples to add (default: 50, only if applyVariations=true)"
    echo "  minLength       : min length for redundant examples (default: 3, only if applyVariations=true)"
    echo "  maxLength       : max length for redundant examples (default: 8, only if applyVariations=true)"
    echo ""
    echo "Examples:"
    echo "  bash run_experiment.sh test_examples/F1_last_2_is_a_ab_dfa.dot"
    echo "  bash run_experiment.sh test_examples/F1_last_2_is_a_ab_dfa.dot 25 1000 true 50 3 8"
    exit 1
fi

DFA_FILE="$1"
MAX_TEST_LEN="${2:-25}"
NUM_TESTS="${3:-1000}"
APPLY_VARIATIONS="${4:-false}"
NUM_ADDITIONAL="${5:-50}"
MIN_LENGTH="${6:-3}"
MAX_LENGTH="${7:-8}"

echo "Configuration:"
echo "  DFA file: $DFA_FILE"
echo "  Max test length: $MAX_TEST_LEN"
echo "  Number of tests: $NUM_TESTS"
echo "  Apply variations: $APPLY_VARIATIONS"
if [ "$APPLY_VARIATIONS" = "true" ]; then
    echo "  Additional examples: $NUM_ADDITIONAL"
    echo "  Length range: $MIN_LENGTH-$MAX_LENGTH"
fi
echo ""

# Compile all necessary classes
echo "Compiling..."
javac -cp .:commons-math3-3.6.1.jar src/*.java -d . 2>&1 | grep -v "Note:"
if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi
echo "Compilation successful!"
echo ""

# Run experiment
echo "Running experiment..."
echo "===================="
if [ "$APPLY_VARIATIONS" = "true" ]; then
    java -cp .:commons-math3-3.6.1.jar DFAToPassiveLearningExperiment "$DFA_FILE" "$MAX_TEST_LEN" "$NUM_TESTS" true "$NUM_ADDITIONAL" "$MIN_LENGTH" "$MAX_LENGTH"
else
    java -cp .:commons-math3-3.6.1.jar DFAToPassiveLearningExperiment "$DFA_FILE" "$MAX_TEST_LEN" "$NUM_TESTS"
fi

EXPERIMENT_STATUS=$?

echo ""
echo "=========================================="
if [ $EXPERIMENT_STATUS -eq 0 ]; then
    echo "Experiment completed successfully!"
else
    echo "Experiment failed with exit code: $EXPERIMENT_STATUS"
fi
echo "=========================================="

exit $EXPERIMENT_STATUS

