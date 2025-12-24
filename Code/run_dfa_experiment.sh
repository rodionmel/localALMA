#!/bin/bash
# DFA Learning Experiment Script
# 
# This script runs the full experiment pipeline:
# 1. Learn from DFA using CharacteristicSetGeneratingOracle (collects queries)
# 2. Create base examples
# 3. Create shuffled + redundant variations
# 4. Test with PassiveLearningOracle
# 5. Output results and learned M2MA
#
# Usage: ./run_dfa_experiment.sh <dfa.dot> [maxTestLen] [numAdditional] [minLength] [maxLength]
#
# Arguments:
#   dfa.dot       : Graphviz DOT file with DFA definition (required)
#   maxTestLen    : max length for EQ test strings (default: 10)
#   numAdditional : number of redundant examples to add (default: 50)
#   minLength     : min length for redundant examples (default: 3)
#   maxLength     : max length for redundant examples (default: 8)

# Get script directory (absolute path)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Capture original working directory before changing
ORIGINAL_DIR="$(pwd)"
# Change to script directory for execution
cd "$SCRIPT_DIR"

# Load Java 11
module load Java/11.0.16 2>&1 > /dev/null

# Check arguments
if [ $# -lt 1 ]; then
    echo "Usage: $0 <dfa.dot> [maxTestLen] [numAdditional] [minLength] [maxLength]"
    echo "  dfa.dot       : Graphviz DOT file with DFA definition (required)"
    echo "  maxTestLen    : max length for EQ test strings (default: 10)"
    echo "  numAdditional : number of redundant examples to add (default: 50)"
    echo "  minLength     : min length for redundant examples (default: 3)"
    echo "  maxLength     : max length for redundant examples (default: 8)"
    echo ""
    echo "Example: $0 test_examples/F1_last_2_is_a_ab_dfa.dot 10 50 3 8"
    exit 1
fi

DFA_FILE="$1"
MAX_TEST_LEN="${2:-10}"
NUM_ADDITIONAL="${3:-50}"
MIN_LENGTH="${4:-3}"
MAX_LENGTH="${5:-8}"

# Convert to absolute path if relative
if [[ ! "$DFA_FILE" = /* ]]; then
    # Try relative to original working directory first (where script was called from)
    if [ -f "$ORIGINAL_DIR/$DFA_FILE" ]; then
        DFA_FILE="$(cd "$ORIGINAL_DIR" && cd "$(dirname "$DFA_FILE")" && pwd)/$(basename "$DFA_FILE")"
    # Then try relative to script directory
    elif [ -f "$SCRIPT_DIR/$DFA_FILE" ]; then
        DFA_FILE="$(cd "$SCRIPT_DIR" && cd "$(dirname "$DFA_FILE")" && pwd)/$(basename "$DFA_FILE")"
    # Then try as-is (might be relative to current dir)
    elif [ -f "$DFA_FILE" ]; then
        DFA_FILE="$(cd "$(dirname "$DFA_FILE")" && pwd)/$(basename "$DFA_FILE")"
    else
        echo "Error: DFA file not found: $DFA_FILE"
        echo "  Original directory: $ORIGINAL_DIR"
        echo "  Script directory: $SCRIPT_DIR"
        echo "  Tried: $ORIGINAL_DIR/$DFA_FILE"
        echo "  Tried: $SCRIPT_DIR/$DFA_FILE"
        echo "  Tried: $DFA_FILE"
        exit 1
    fi
fi

# Check if DFA file exists (final check)
if [ ! -f "$DFA_FILE" ]; then
    echo "Error: DFA file not found: $DFA_FILE"
    exit 1
fi

echo "=========================================="
echo "DFA Learning Experiment"
echo "=========================================="
echo ""
echo "Configuration:"
echo "  DFA file: $DFA_FILE"
echo "  Max test length: $MAX_TEST_LEN"
echo "  Additional examples: $NUM_ADDITIONAL"
echo "  Length range: $MIN_LENGTH-$MAX_LENGTH"
echo "  Working directory: $SCRIPT_DIR"
echo ""

# Compile if needed
echo "Checking compilation..."
if [ ! -f "DFAToPassiveLearningExperiment.class" ] || \
   [ "src/DFAToPassiveLearningExperiment.java" -nt "DFAToPassiveLearningExperiment.class" ]; then
    echo "Compiling..."
    javac -cp .:commons-math3-3.6.1.jar src/*.java -d . 2>&1 | grep -v "Note:" | head -20
    if [ $? -ne 0 ]; then
        echo "Compilation failed!"
        exit 1
    fi
    echo "Compilation successful!"
else
    echo "Using existing compiled classes."
fi
echo ""

# Run experiment with variations
echo "Running experiment with variations..."
echo "======================================"
echo ""

java -cp .:commons-math3-3.6.1.jar DFAToPassiveLearningExperiment \
    "$DFA_FILE" \
    "$MAX_TEST_LEN" \
    1000 \
    true \
    "$NUM_ADDITIONAL" \
    "$MIN_LENGTH" \
    "$MAX_LENGTH"

EXPERIMENT_STATUS=$?

echo ""
echo "=========================================="
if [ $EXPERIMENT_STATUS -eq 0 ]; then
    echo "Experiment completed successfully!"
    
    # Show output files
    DFA_BASE=$(basename "$DFA_FILE" .dot)
    echo ""
    echo "Generated files:"
    echo "  Base queries: ${DFA_BASE}_queries.json"
    echo "  Shuffled: ${DFA_BASE}_queries_shuffled.json"
    echo "  With redundancy: ${DFA_BASE}_queries_shuffled_redundant.json"
else
    echo "Experiment failed with exit code: $EXPERIMENT_STATUS"
fi
echo "=========================================="

exit $EXPERIMENT_STATUS

