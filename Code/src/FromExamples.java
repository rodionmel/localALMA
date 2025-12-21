/*
 * FromExamples.java
 * 
 * Learns an M2MA directly from positive and negative example words,
 * using the Hankel matrix construction approach.
 * 
 * Unlike the interactive learning algorithm, this does not require an oracle
 * that can answer arbitrary membership queries. Instead, it:
 * 1. Extracts prefixes and suffixes from the examples
 * 2. Builds a partial Hankel matrix from known labels
 * 3. Checks if the information is sufficient (closure property)
 * 4. Constructs the M2MA directly if possible
 * 
 * If information is insufficient, it reports which words need labels.
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class FromExamples {
    
    // Examples
    public static Set<String> positiveWords = new HashSet<>();
    public static Set<String> negativeWords = new HashSet<>();
    public static Set<String> allWords = new HashSet<>();
    
    // Alphabet
    public static String[] alphabet;
    public static Map<String, Integer> letterToIndex = new HashMap<>();
    
    // Prefixes and suffixes extracted from examples
    public static List<String> prefixes = new ArrayList<>();
    public static List<String> suffixes = new ArrayList<>();
    public static Set<String> prefixSet = new HashSet<>();
    public static Set<String> suffixSet = new HashSet<>();
    
    // Hankel matrix: hankel[prefixIndex][suffixIndex] = 0, 1, or -1 (unknown)
    public static int[][] hankel;
    
    // Basis rows (linearly independent)
    public static List<Integer> basisRowIndices = new ArrayList<>();
    public static List<int[]> basisRows = new ArrayList<>();
    
    // Result M2MA
    public static int dimension;
    public static int[] finalVector;
    public static int[][][] transitionMatrices; // [letterIndex][row][col]
    
    // Verbose mode
    public static boolean verbose = false;
    
    // Closed-world assumption: assume unknown words are negative
    public static boolean closedWorld = false;
    
    public static void main(String[] args) throws Exception {
        System.out.println("FromExamples: Learn M2MA from Positive/Negative Examples");
        System.out.println("=========================================================\n");
        
        if (args.length < 1) {
            System.out.println("Usage: java FromExamples <input.json> [-v] [-c]");
            System.out.println("  -v : verbose mode");
            System.out.println("  -c : closed-world assumption (unknown words are negative)");
            System.exit(1);
        }
        
        String inputFile = args[0];
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("-v")) verbose = true;
            if (args[i].equals("-c")) closedWorld = true;
        }
        
        // Step 1: Load examples from JSON
        System.out.println("Step 1: Loading examples from " + inputFile);
        loadExamplesFromJSON(inputFile);
        System.out.println("  Alphabet: " + Arrays.toString(alphabet));
        System.out.println("  Positive examples: " + positiveWords.size());
        System.out.println("  Negative examples: " + negativeWords.size());
        if (closedWorld) {
            System.out.println("  Mode: CLOSED-WORLD (unknown words assumed negative)");
        }
        System.out.println();
        
        // Step 2: Extract prefixes and suffixes
        System.out.println("Step 2: Extracting prefixes and suffixes");
        extractPrefixesAndSuffixes();
        System.out.println("  Prefixes: " + prefixes.size());
        System.out.println("  Suffixes: " + suffixes.size());
        if (verbose) {
            System.out.println("  Prefix list: " + formatWordList(prefixes));
            System.out.println("  Suffix list: " + formatWordList(suffixes));
        }
        System.out.println();
        
        // Step 3: Build Hankel matrix
        System.out.println("Step 3: Building Hankel matrix");
        buildHankelMatrix();
        int unknownCount = countUnknown();
        System.out.println("  Matrix size: " + prefixes.size() + " x " + suffixes.size());
        System.out.println("  Known entries: " + (prefixes.size() * suffixes.size() - unknownCount));
        System.out.println("  Unknown entries: " + unknownCount);
        if (verbose) {
            printHankelMatrix();
        }
        System.out.println();
        
        // Step 4: Find linearly independent rows (basis)
        System.out.println("Step 4: Finding linearly independent rows (basis)");
        findBasis();
        System.out.println("  Basis size (M2MA dimension): " + dimension);
        System.out.println("  Basis rows: " + formatBasisRows());
        System.out.println();
        
        // Step 5: Check closure property
        System.out.println("Step 5: Checking closure property");
        List<String> missingWords = checkClosure();
        if (!missingWords.isEmpty()) {
            System.out.println("\n*** INSUFFICIENT INFORMATION ***");
            System.out.println("The following words need labels (positive or negative):");
            for (String w : missingWords) {
                System.out.println("  " + (w.isEmpty() ? "ε (empty string)" : w));
            }
            System.out.println("\nPlease add these words to your examples and re-run.");
            System.exit(1);
        }
        System.out.println("  Closure property satisfied!");
        System.out.println();
        
        // Step 6: Construct M2MA
        System.out.println("Step 6: Constructing M2MA");
        constructM2MA();
        System.out.println();
        
        // Step 7: Display result
        displayM2MA();
        
        // Step 8: Verify against all examples
        System.out.println("Step 7: Verifying against all examples");
        verify();
    }
    
    /**
     * Load examples from JSON file.
     * Expected format:
     * {
     *   "metadata": { "alphabet": ["a", "b"], ... },
     *   "Positive sample": ["", "a a", ...],
     *   "Negative sample": ["a", "b", ...]
     * }
     */
    public static void loadExamplesFromJSON(String filename) throws Exception {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        reader.close();
        
        String json = content.toString();
        
        // Parse alphabet from metadata
        List<String> alphabetList = new ArrayList<>();
        int alphaStart = json.indexOf("\"alphabet\"");
        if (alphaStart != -1) {
            int arrayStart = json.indexOf("[", alphaStart);
            int arrayEnd = json.indexOf("]", arrayStart);
            String alphaArray = json.substring(arrayStart + 1, arrayEnd);
            String[] parts = alphaArray.split(",");
            for (String part : parts) {
                String letter = part.trim().replace("\"", "").trim();
                if (!letter.isEmpty()) {
                    alphabetList.add(letter);
                }
            }
        }
        alphabet = alphabetList.toArray(new String[0]);
        for (int i = 0; i < alphabet.length; i++) {
            letterToIndex.put(alphabet[i], i);
        }
        
        // Parse positive samples
        int posStart = json.indexOf("\"Positive sample\"");
        if (posStart != -1) {
            int arrayStart = json.indexOf("[", posStart);
            int arrayEnd = findMatchingBracket(json, arrayStart);
            parseWordArray(json.substring(arrayStart + 1, arrayEnd), positiveWords);
        }
        
        // Parse negative samples
        int negStart = json.indexOf("\"Negative sample\"");
        if (negStart != -1) {
            int arrayStart = json.indexOf("[", negStart);
            int arrayEnd = findMatchingBracket(json, arrayStart);
            parseWordArray(json.substring(arrayStart + 1, arrayEnd), negativeWords);
        }
        
        allWords.addAll(positiveWords);
        allWords.addAll(negativeWords);
    }
    
    private static int findMatchingBracket(String s, int start) {
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) == '[') depth++;
            else if (s.charAt(i) == ']') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return s.length() - 1;
    }
    
    private static void parseWordArray(String arrayContent, Set<String> target) {
        // Split by comma, but handle quoted strings
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        boolean foundQuote = false; // Track if we've seen any quote for this entry
        
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
                foundQuote = true;
            } else if (c == ',' && !inQuote) {
                // Add the word if we found a quoted string (even if empty)
                if (foundQuote) {
                    target.add(current.toString());
                }
                current = new StringBuilder();
                foundQuote = false;
            } else if (inQuote) {
                current.append(c);
            }
        }
        // Don't forget the last word
        if (foundQuote) {
            target.add(current.toString());
        }
    }
    
    /**
     * Extract all prefixes and suffixes from the example words.
     * Automatically detects maximum length with full coverage and restricts suffixes.
     */
    public static void extractPrefixesAndSuffixes() {
        // First, determine the maximum word length with full coverage
        int maxFullCoverageLen = findMaxFullCoverageLength();
        System.out.println("  Max length with full coverage: " + maxFullCoverageLen);
        
        // Always include empty string
        prefixSet.add("");
        suffixSet.add("");
        
        // Include single letters
        for (String letter : alphabet) {
            prefixSet.add(letter);
            suffixSet.add(letter);
        }
        
        // Extract from all words
        for (String word : allWords) {
            String[] parts = word.isEmpty() ? new String[0] : word.split(" ");
            
            // All prefixes (we need all of them for the algorithm)
            StringBuilder prefix = new StringBuilder();
            prefixSet.add(""); // empty prefix
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) prefix.append(" ");
                prefix.append(parts[i]);
                prefixSet.add(prefix.toString());
            }
            
            // Suffixes - only up to max coverage length for the Hankel columns
            for (int i = 0; i < parts.length; i++) {
                int suffixLen = parts.length - i;
                if (suffixLen <= maxFullCoverageLen) {
                    StringBuilder suffix = new StringBuilder();
                    for (int j = i; j < parts.length; j++) {
                        if (j > i) suffix.append(" ");
                        suffix.append(parts[j]);
                    }
                    suffixSet.add(suffix.toString());
                }
            }
        }
        
        // Also generate all possible suffixes up to maxFullCoverageLen
        generateAllWords(maxFullCoverageLen, suffixSet);
        
        // Convert to sorted lists
        prefixes = new ArrayList<>(prefixSet);
        suffixes = new ArrayList<>(suffixSet);
        
        // Sort by length, then alphabetically (empty string first)
        Comparator<String> comp = (a, b) -> {
            int lenA = a.isEmpty() ? 0 : a.split(" ").length;
            int lenB = b.isEmpty() ? 0 : b.split(" ").length;
            if (lenA != lenB) return lenA - lenB;
            return a.compareTo(b);
        };
        Collections.sort(prefixes, comp);
        Collections.sort(suffixes, comp);
    }
    
    /**
     * Find the maximum word length where we have labels for ALL possible words.
     */
    public static int findMaxFullCoverageLength() {
        int maxLen = 0;
        for (String word : allWords) {
            int len = word.isEmpty() ? 0 : word.split(" ").length;
            if (len > maxLen) maxLen = len;
        }
        
        // Check each length for full coverage
        for (int len = 0; len <= maxLen; len++) {
            int expected = (int) Math.pow(alphabet.length, len);
            int found = 0;
            
            // Count words of this length in our samples
            for (String word : allWords) {
                int wordLen = word.isEmpty() ? 0 : word.split(" ").length;
                if (wordLen == len) found++;
            }
            
            if (found < expected) {
                // This length doesn't have full coverage
                return len - 1;
            }
        }
        
        return maxLen;
    }
    
    /**
     * Generate all possible words up to given length.
     */
    public static void generateAllWords(int maxLen, Set<String> target) {
        target.add(""); // empty string
        
        List<String> current = new ArrayList<>();
        current.add("");
        
        for (int len = 1; len <= maxLen; len++) {
            List<String> next = new ArrayList<>();
            for (String word : current) {
                for (String letter : alphabet) {
                    String newWord = word.isEmpty() ? letter : word + " " + letter;
                    target.add(newWord);
                    next.add(newWord);
                }
            }
            current = next;
        }
    }
    
    /**
     * Build the Hankel matrix from known labels.
     */
    public static void buildHankelMatrix() {
        int numPrefixes = prefixes.size();
        int numSuffixes = suffixes.size();
        hankel = new int[numPrefixes][numSuffixes];
        
        for (int i = 0; i < numPrefixes; i++) {
            for (int j = 0; j < numSuffixes; j++) {
                String word = concatenate(prefixes.get(i), suffixes.get(j));
                hankel[i][j] = lookup(word);
            }
        }
    }
    
    /**
     * Lookup a word's label.
     * @return 1 if positive, 0 if negative, -1 if unknown (or 0 in closed-world mode)
     */
    public static int lookup(String word) {
        if (positiveWords.contains(word)) return 1;
        if (negativeWords.contains(word)) return 0;
        if (closedWorld) return 0; // Assume unknown words are negative
        return -1; // unknown
    }
    
    /**
     * Concatenate two words with proper spacing.
     */
    public static String concatenate(String a, String b) {
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;
        return a + " " + b;
    }
    
    // Pivot tracking for basis
    public static int[] pivotCol;
    
    /**
     * Find linearly independent rows to form the basis.
     * Uses GF(2) arithmetic.
     */
    public static void findBasis() {
        basisRowIndices.clear();
        basisRows.clear();
        
        // For tracking which columns have pivots
        pivotCol = new int[suffixes.size()]; // -1 means no pivot
        Arrays.fill(pivotCol, -1);
        
        for (int i = 0; i < prefixes.size(); i++) {
            tryAddToBasis(i);
        }
        
        dimension = basisRows.size();
        
        // Handle edge case: all zeros
        if (dimension == 0) {
            // Add empty row as trivial basis
            basisRowIndices.add(0);
            basisRows.add(new int[suffixes.size()]);
            dimension = 1;
        }
    }
    
    /**
     * Try to add a prefix row to the basis if it's linearly independent.
     * @return true if added, false if it was dependent on existing basis
     */
    public static boolean tryAddToBasis(int prefixIdx) {
        int[] row = hankel[prefixIdx].clone();
        
        // Check if row has any unknown entries
        boolean hasUnknown = false;
        for (int val : row) {
            if (val == -1) {
                hasUnknown = true;
                break;
            }
        }
        
        // Skip rows with unknown entries
        if (hasUnknown) return false;
        
        // Reduce by existing basis rows
        for (int b = 0; b < basisRows.size(); b++) {
            int[] basisRow = basisRows.get(b);
            int pivotColumn = -1;
            for (int c = 0; c < suffixes.size(); c++) {
                if (basisRow[c] == 1 && pivotCol[c] == b) {
                    pivotColumn = c;
                    break;
                }
            }
            if (pivotColumn != -1 && row[pivotColumn] == 1) {
                // XOR with basis row
                for (int c = 0; c < suffixes.size(); c++) {
                    row[c] ^= basisRow[c];
                }
            }
        }
        
        // Check if row is non-zero (linearly independent)
        int firstOne = -1;
        for (int c = 0; c < suffixes.size(); c++) {
            if (row[c] == 1) {
                firstOne = c;
                break;
            }
        }
        
        if (firstOne != -1) {
            basisRowIndices.add(prefixIdx);
            basisRows.add(row);
            pivotCol[firstOne] = basisRows.size() - 1;
            dimension = basisRows.size();
            return true;
        }
        
        return false;
    }
    
    /**
     * Ensure a prefix is in the basis (add if linearly independent).
     * @return the index of this prefix in the basis, or -1 if dependent
     */
    public static int ensureInBasis(String prefix) {
        int prefixIdx = prefixes.indexOf(prefix);
        if (prefixIdx == -1) {
            // Add prefix to the list
            prefixes.add(prefix);
            prefixSet.add(prefix);
            prefixIdx = prefixes.size() - 1;
            
            // Extend Hankel matrix
            int[][] newHankel = new int[prefixes.size()][suffixes.size()];
            for (int i = 0; i < hankel.length; i++) {
                newHankel[i] = hankel[i];
            }
            // Fill new row
            for (int j = 0; j < suffixes.size(); j++) {
                String word = concatenate(prefix, suffixes.get(j));
                newHankel[prefixIdx][j] = lookup(word);
            }
            hankel = newHankel;
        }
        
        // Check if already in basis
        for (int i = 0; i < basisRowIndices.size(); i++) {
            if (basisRowIndices.get(i) == prefixIdx) {
                return i;
            }
        }
        
        // Try to add to basis
        if (tryAddToBasis(prefixIdx)) {
            return basisRowIndices.size() - 1;
        }
        
        return -1; // dependent on existing basis
    }
    
    /**
     * Check the closure property.
     * For each basis row u and letter σ, we need the row for uσ
     * to be expressible as a linear combination of basis rows.
     * 
     * @return List of words that need labels (empty if closure is satisfied)
     */
    public static List<String> checkClosure() {
        Set<String> missing = new LinkedHashSet<>();
        
        for (int basisIdx : basisRowIndices) {
            String u = prefixes.get(basisIdx);
            
            for (String sigma : alphabet) {
                String uSigma = concatenate(u, sigma);
                
                // Find or check this row
                int rowIdx = prefixes.indexOf(uSigma);
                
                if (rowIdx == -1) {
                    // This prefix is not in our prefix set - need to add it
                    // and check all its suffix combinations
                    for (String suffix : suffixes) {
                        String word = concatenate(uSigma, suffix);
                        if (lookup(word) == -1) {
                            missing.add(word);
                        }
                    }
                } else {
                    // Check if this row has all known entries
                    for (int j = 0; j < suffixes.size(); j++) {
                        if (hankel[rowIdx][j] == -1) {
                            String word = concatenate(uSigma, suffixes.get(j));
                            missing.add(word);
                        }
                    }
                }
            }
        }
        
        return new ArrayList<>(missing);
    }
    
    /**
     * Construct the M2MA from the Hankel matrix.
     * Uses an iterative approach: extend basis until all transitions can be expressed.
     */
    public static void constructM2MA() throws Exception {
        boolean changed = true;
        int iterations = 0;
        int maxIterations = 100;
        
        while (changed && iterations < maxIterations) {
            changed = false;
            iterations++;
            
            // Ensure we have all extensions of basis rows
            List<Integer> currentBasis = new ArrayList<>(basisRowIndices);
            for (int basisIdx : currentBasis) {
                String u = prefixes.get(basisIdx);
                for (String sigma : alphabet) {
                    String uSigma = concatenate(u, sigma);
                    
                    // Ensure uSigma is in prefixes and has Hankel row
                    int uSigmaIdx = prefixes.indexOf(uSigma);
                    if (uSigmaIdx == -1) {
                        // Add new prefix
                        prefixes.add(uSigma);
                        prefixSet.add(uSigma);
                        uSigmaIdx = prefixes.size() - 1;
                        
                        // Extend Hankel matrix
                        int[][] newHankel = new int[prefixes.size()][suffixes.size()];
                        for (int i = 0; i < hankel.length; i++) {
                            newHankel[i] = hankel[i];
                        }
                        for (int j = 0; j < suffixes.size(); j++) {
                            String word = concatenate(uSigma, suffixes.get(j));
                            newHankel[uSigmaIdx][j] = lookup(word);
                        }
                        hankel = newHankel;
                    }
                    
                    // Try to express uSigma row as combination of basis rows
                    int[] targetRow = hankel[uSigmaIdx];
                    boolean canExpress = canExpressAsLinearCombination(targetRow);
                    
                    if (!canExpress) {
                        // Add to basis
                        if (verbose) {
                            System.out.println("  Trying to extend basis with: " + uSigma + " (idx=" + uSigmaIdx + ")");
                        }
                        if (tryAddToBasis(uSigmaIdx)) {
                            changed = true;
                            if (verbose) {
                                System.out.println("    -> Added successfully! New dimension: " + dimension);
                            }
                        } else {
                            if (verbose) {
                                System.out.println("    -> Failed to add (already dependent or has unknowns)");
                            }
                        }
                    }
                }
            }
        }
        
        if (iterations >= maxIterations) {
            System.out.println("  Warning: reached max iterations in basis extension");
        }
        
        System.out.println("  Final dimension: " + dimension);
        System.out.println("  Final basis: " + formatBasisRows());
        
        // Now construct the final vector and transition matrices
        finalVector = new int[dimension];
        int emptyColIdx = suffixes.indexOf("");
        for (int i = 0; i < dimension; i++) {
            int rowIdx = basisRowIndices.get(i);
            finalVector[i] = hankel[rowIdx][emptyColIdx];
        }
        
        // Transition matrices
        transitionMatrices = new int[alphabet.length][dimension][dimension];
        
        for (int a = 0; a < alphabet.length; a++) {
            String sigma = alphabet[a];
            
            for (int i = 0; i < dimension; i++) {
                String u = prefixes.get(basisRowIndices.get(i));
                String uSigma = concatenate(u, sigma);
                
                int uSigmaRowIdx = prefixes.indexOf(uSigma);
                int[] targetRow = hankel[uSigmaRowIdx];
                
                // Express as linear combination
                try {
                    int[] coefficients = expressAsLinearCombination(targetRow);
                    for (int j = 0; j < dimension; j++) {
                        transitionMatrices[a][i][j] = coefficients[j];
                    }
                } catch (Exception e) {
                    System.out.println("  ERROR: Cannot express " + uSigma + " (row " + uSigmaRowIdx + ") as linear combination");
                    System.out.print("    Target row: [");
                    for (int v : targetRow) System.out.print(v + " ");
                    System.out.println("]");
                    System.out.println("    Basis rows:");
                    for (int b = 0; b < basisRowIndices.size(); b++) {
                        System.out.print("      " + prefixes.get(basisRowIndices.get(b)) + ": [");
                        for (int v : hankel[basisRowIndices.get(b)]) System.out.print(v + " ");
                        System.out.println("]");
                    }
                    throw e;
                }
            }
        }
    }
    
    /**
     * Check if a row can be expressed as linear combination of basis rows.
     */
    public static boolean canExpressAsLinearCombination(int[] targetRow) {
        int[] current = targetRow.clone();
        
        for (int b = 0; b < basisRowIndices.size(); b++) {
            int basisRowIdx = basisRowIndices.get(b);
            int[] basisRow = hankel[basisRowIdx];
            
            // Find pivot column
            int pivotColumn = -1;
            for (int c = 0; c < suffixes.size(); c++) {
                if (basisRow[c] == 1 && pivotCol[c] == b) {
                    pivotColumn = c;
                    break;
                }
            }
            
            if (pivotColumn != -1 && current[pivotColumn] == 1) {
                for (int c = 0; c < suffixes.size(); c++) {
                    current[c] ^= basisRow[c];
                }
            }
        }
        
        // Check if reduced to zero
        for (int val : current) {
            if (val == 1) return false;
        }
        return true;
    }
    
    /**
     * Express a row as a linear combination of basis rows.
     * Returns the coefficients.
     * Uses the pivotCol array that was set during basis construction.
     */
    public static int[] expressAsLinearCombination(int[] targetRow) throws Exception {
        int[] coefficients = new int[dimension];
        int[] current = targetRow.clone();
        
        // Use the REDUCED basis rows and their pivot columns
        // (must match the reduction process in tryAddToBasis)
        for (int b = 0; b < dimension; b++) {
            // Find the pivot column for this basis row (stored in pivotCol array)
            int pc = -1;
            for (int c = 0; c < suffixes.size(); c++) {
                if (pivotCol[c] == b) {
                    pc = c;
                    break;
                }
            }
            
            if (pc != -1 && current[pc] == 1) {
                coefficients[b] = 1;
                // XOR with the REDUCED basis row (same as used in tryAddToBasis)
                int[] reducedRow = basisRows.get(b);
                for (int c = 0; c < suffixes.size(); c++) {
                    current[c] ^= reducedRow[c];
                }
            }
        }
        
        // Verify: current should now be all zeros
        for (int val : current) {
            if (val == 1) {
                throw new Exception("Failed to express row as linear combination of basis rows");
            }
        }
        
        return coefficients;
    }
    
    /**
     * Display the learned M2MA.
     */
    public static void displayM2MA() {
        System.out.println("Learned M2MA");
        System.out.println("------------");
        System.out.println("Dimension: " + dimension);
        System.out.println();
        
        System.out.print("Final Vector: ");
        for (int i = 0; i < dimension; i++) {
            System.out.print(finalVector[i] + " ");
        }
        System.out.println("\n");
        
        System.out.println("Transition Matrices:");
        for (int a = 0; a < alphabet.length; a++) {
            System.out.println("Letter " + alphabet[a]);
            for (int i = 0; i < dimension; i++) {
                for (int j = 0; j < dimension; j++) {
                    System.out.print(transitionMatrices[a][i][j] + " ");
                }
                System.out.println();
            }
            System.out.println();
        }
    }
    
    /**
     * Verify the learned M2MA against all examples.
     */
    public static void verify() {
        int correctPositive = 0;
        int correctNegative = 0;
        int wrongPositive = 0;
        int wrongNegative = 0;
        
        for (String word : positiveWords) {
            if (evaluate(word) == 1) {
                correctPositive++;
            } else {
                wrongPositive++;
                if (verbose) System.out.println("  WRONG: '" + word + "' should be positive");
            }
        }
        
        for (String word : negativeWords) {
            if (evaluate(word) == 0) {
                correctNegative++;
            } else {
                wrongNegative++;
                if (verbose) System.out.println("  WRONG: '" + word + "' should be negative");
            }
        }
        
        System.out.println("  Positive examples: " + correctPositive + "/" + positiveWords.size() + " correct");
        System.out.println("  Negative examples: " + correctNegative + "/" + negativeWords.size() + " correct");
        
        if (wrongPositive == 0 && wrongNegative == 0) {
            System.out.println("\n*** SUCCESS: M2MA correctly classifies all examples! ***");
        } else {
            System.out.println("\n*** WARNING: " + (wrongPositive + wrongNegative) + " examples misclassified ***");
        }
    }
    
    /**
     * Evaluate a word using the learned M2MA.
     * Computes: initial_vector × μ(w) × final_vector
     * where initial_vector = (1, 0, 0, ..., 0)
     */
    public static int evaluate(String word) {
        // Start with initial vector [1, 0, 0, ..., 0]
        int[] state = new int[dimension];
        state[0] = 1;
        
        // Parse word
        String[] letters = word.isEmpty() ? new String[0] : word.split(" ");
        
        // Apply transition matrices
        for (String letter : letters) {
            int letterIdx = letterToIndex.get(letter);
            int[] newState = new int[dimension];
            
            for (int i = 0; i < dimension; i++) {
                if (state[i] == 1) {
                    for (int j = 0; j < dimension; j++) {
                        newState[j] ^= transitionMatrices[letterIdx][i][j];
                    }
                }
            }
            state = newState;
        }
        
        // Dot product with final vector
        int result = 0;
        for (int i = 0; i < dimension; i++) {
            result ^= (state[i] & finalVector[i]);
        }
        
        return result;
    }
    
    // Helper methods for display
    
    private static int countUnknown() {
        int count = 0;
        for (int i = 0; i < prefixes.size(); i++) {
            for (int j = 0; j < suffixes.size(); j++) {
                if (hankel[i][j] == -1) count++;
            }
        }
        return count;
    }
    
    private static String formatWordList(List<String> words) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < Math.min(words.size(), 10); i++) {
            if (i > 0) sb.append(", ");
            sb.append(words.get(i).isEmpty() ? "ε" : words.get(i));
        }
        if (words.size() > 10) sb.append(", ... (" + words.size() + " total)");
        sb.append("]");
        return sb.toString();
    }
    
    private static String formatBasisRows() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < basisRowIndices.size(); i++) {
            if (i > 0) sb.append(", ");
            String prefix = prefixes.get(basisRowIndices.get(i));
            sb.append(prefix.isEmpty() ? "ε" : prefix);
        }
        sb.append("]");
        return sb.toString();
    }
    
    private static void printHankelMatrix() {
        System.out.println("\n  Hankel Matrix:");
        System.out.print("         ");
        for (int j = 0; j < Math.min(suffixes.size(), 10); j++) {
            String s = suffixes.get(j).isEmpty() ? "ε" : suffixes.get(j);
            System.out.printf("%6s ", s.length() > 5 ? s.substring(0, 5) : s);
        }
        if (suffixes.size() > 10) System.out.print("...");
        System.out.println();
        
        for (int i = 0; i < Math.min(prefixes.size(), 15); i++) {
            String p = prefixes.get(i).isEmpty() ? "ε" : prefixes.get(i);
            System.out.printf("%8s ", p.length() > 7 ? p.substring(0, 7) : p);
            for (int j = 0; j < Math.min(suffixes.size(), 10); j++) {
                String val = hankel[i][j] == -1 ? "?" : String.valueOf(hankel[i][j]);
                System.out.printf("%6s ", val);
            }
            if (suffixes.size() > 10) System.out.print("...");
            System.out.println();
        }
        if (prefixes.size() > 15) System.out.println("  ... (" + prefixes.size() + " rows total)");
    }
}

