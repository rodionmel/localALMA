/*
 * LearnFromExamples.java
 * 
 * Modified M2MA learning algorithm that only queries words from the provided examples.
 * Based on the original M2MA.java but adapted to work with finite example sets.
 * 
 * Key differences from standard L* learning:
 * - Only uses prefixes/suffixes that appear in the examples
 * - MQ can only answer queries for words in the example set
 * - EQ checks against all known examples (not random sampling)
 * - Counterexamples come from misclassified examples
 * 
 * Usage: java LearnFromExamples <input.json> [-v]
 */

import java.io.*;
import java.util.*;
import org.apache.commons.math3.linear.*;

public class LearnFromExamples {
    
    // Examples
    public static Set<String> positiveWords = new HashSet<>();
    public static Set<String> negativeWords = new HashSet<>();
    public static Set<String> allWords = new HashSet<>();
    
    // Alphabet
    public static String[] alphabet;
    public static Map<String, Integer> letterToIndex = new HashMap<>();
    
    // Available prefixes and suffixes (extracted from examples)
    public static List<String> availablePrefixes = new ArrayList<>();
    public static List<String> availableSuffixes = new ArrayList<>();
    public static Set<String> prefixSet = new HashSet<>();
    public static Set<String> suffixSet = new HashSet<>();
    
    // Learning state
    public static ArrayList<String> rowIndices = new ArrayList<>();
    public static ArrayList<String> colIndices = new ArrayList<>();
    public static int dimension;
    
    // Hankel cache
    public static Map<String, Integer> Hankel = new HashMap<>();
    
    // Result M2MA
    public static HashMap<Integer, ArrayList<Integer>> resultFinalVector;
    public static HashMap<Integer, ArrayList<Integer>>[] resultTransitionMatrices;
    
    // Counterexample from EQ
    public static String counterExample;
    
    // Verbose mode
    public static boolean verbose = false;
    
    public static void main(String[] args) throws Exception {
        System.out.println("LearnFromExamples: M2MA Learning with Finite Examples");
        System.out.println("======================================================\n");
        
        if (args.length < 1) {
            System.out.println("Usage: java LearnFromExamples <input.json> [-v]");
            System.out.println("  -v : verbose mode");
            System.exit(1);
        }
        
        String inputFile = args[0];
        verbose = args.length > 1 && args[1].equals("-v");
        
        long startTime = System.nanoTime();
        
        // Step 1: Load examples
        System.out.println("Step 1: Loading examples from " + inputFile);
        loadExamplesFromJSON(inputFile);
        System.out.println("  Alphabet: " + Arrays.toString(alphabet));
        System.out.println("  Positive examples: " + positiveWords.size());
        System.out.println("  Negative examples: " + negativeWords.size());
        System.out.println();
        
        // Step 2: Extract available prefixes and suffixes
        System.out.println("Step 2: Extracting prefixes and suffixes from examples");
        extractPrefixesSuffixes();
        System.out.println("  Available prefixes: " + availablePrefixes.size());
        System.out.println("  Available suffixes: " + availableSuffixes.size());
        System.out.println();
        
        // Step 3: Learn the M2MA
        System.out.println("Step 3: Learning M2MA");
        System.out.println("---------------------");
        learn();
        System.out.println();
        
        // Step 4: Minimize
        System.out.println("Step 4: Minimizing");
        minimize();
        System.out.println();
        
        // Step 5: Display result
        displayResults();
        
        long endTime = System.nanoTime();
        double totalTime = (endTime - startTime) / 1e9;
        System.out.println("Ran in " + String.format("%.2f", totalTime) + "s.\n");
        
        // Step 6: Verify
        System.out.println("Step 5: Verifying against all examples");
        verify();
        
        // Interactive testing
        System.out.println("\nEnter words to test (or 'quit' to exit):");
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String word = scanner.nextLine().trim();
            if (word.equals("quit")) break;
            int result = evaluateM2MA(word);
            System.out.println(result == 1 ? "Accepted" : "Not accepted");
        }
        scanner.close();
    }
    
    // ==================== JSON Loading ====================
    
    public static void loadExamplesFromJSON(String filename) throws Exception {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        reader.close();
        
        String json = content.toString();
        
        // Parse alphabet
        List<String> alphabetList = new ArrayList<>();
        int alphaStart = json.indexOf("\"alphabet\"");
        if (alphaStart != -1) {
            int arrayStart = json.indexOf("[", alphaStart);
            int arrayEnd = json.indexOf("]", arrayStart);
            String alphaArray = json.substring(arrayStart + 1, arrayEnd);
            for (String part : alphaArray.split(",")) {
                String letter = part.trim().replace("\"", "").trim();
                if (!letter.isEmpty()) alphabetList.add(letter);
            }
        }
        alphabet = alphabetList.toArray(new String[0]);
        for (int i = 0; i < alphabet.length; i++) {
            letterToIndex.put(alphabet[i], i);
        }
        
        // Parse positive samples
        positiveWords.clear();
        int posStart = json.indexOf("\"Positive sample\"");
        if (posStart != -1) {
            int arrayStart = json.indexOf("[", posStart);
            int arrayEnd = findMatchingBracket(json, arrayStart);
            parseWordArray(json.substring(arrayStart + 1, arrayEnd), positiveWords);
        }
        
        // Parse negative samples
        negativeWords.clear();
        int negStart = json.indexOf("\"Negative sample\"");
        if (negStart != -1) {
            int arrayStart = json.indexOf("[", negStart);
            int arrayEnd = findMatchingBracket(json, arrayStart);
            parseWordArray(json.substring(arrayStart + 1, arrayEnd), negativeWords);
        }
        
        allWords.addAll(positiveWords);
        allWords.addAll(negativeWords);
        
        // Pre-populate Hankel cache
        for (String w : positiveWords) Hankel.put(w, 1);
        for (String w : negativeWords) Hankel.put(w, 0);
    }
    
    private static int findMatchingBracket(String s, int start) {
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) == '[') depth++;
            else if (s.charAt(i) == ']') { depth--; if (depth == 0) return i; }
        }
        return s.length() - 1;
    }
    
    private static void parseWordArray(String content, Set<String> target) {
        StringBuilder current = new StringBuilder();
        boolean inQuote = false, foundQuote = false;
        for (char c : content.toCharArray()) {
            if (c == '"') { inQuote = !inQuote; foundQuote = true; }
            else if (c == ',' && !inQuote) {
                if (foundQuote) target.add(current.toString());
                current = new StringBuilder(); foundQuote = false;
            }
            else if (inQuote) current.append(c);
        }
        if (foundQuote) target.add(current.toString());
    }
    
    // ==================== Prefix/Suffix Extraction ====================
    
    public static void extractPrefixesSuffixes() {
        prefixSet.add("");
        suffixSet.add("");
        
        for (String word : allWords) {
            String[] parts = word.isEmpty() ? new String[0] : word.split(" ");
            
            // Extract all prefixes
            StringBuilder prefix = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) prefix.append(" ");
                prefix.append(parts[i]);
                prefixSet.add(prefix.toString());
            }
            
            // Extract all suffixes
            for (int i = 0; i < parts.length; i++) {
                StringBuilder suffix = new StringBuilder();
                for (int j = i; j < parts.length; j++) {
                    if (j > i) suffix.append(" ");
                    suffix.append(parts[j]);
                }
                suffixSet.add(suffix.toString());
            }
        }
        
        // Sort by length
        availablePrefixes = new ArrayList<>(prefixSet);
        availableSuffixes = new ArrayList<>(suffixSet);
        Comparator<String> byLength = (a, b) -> {
            int la = a.isEmpty() ? 0 : a.split(" ").length;
            int lb = b.isEmpty() ? 0 : b.split(" ").length;
            return la != lb ? la - lb : a.compareTo(b);
        };
        Collections.sort(availablePrefixes, byLength);
        Collections.sort(availableSuffixes, byLength);
    }
    
    // ==================== Membership Query ====================
    
    /**
     * Membership query - returns 1 if accepted, 0 if rejected.
     * Can only answer for words in the example set.
     * For unknown words, tries to find equivalent information.
     */
    public static int MQ(String word) throws Exception {
        // Check cache first
        if (Hankel.containsKey(word)) {
            return Hankel.get(word);
        }
        
        // Word not in examples - try to infer
        // Strategy 1: Check if word is directly in positive/negative sets
        if (positiveWords.contains(word)) {
            Hankel.put(word, 1);
            return 1;
        }
        if (negativeWords.contains(word)) {
            Hankel.put(word, 0);
            return 0;
        }
        
        // Strategy 2: Word not in examples - this is a problem
        // For now, throw an exception to identify which words are needed
        throw new UnknownWordException(word);
    }
    
    /**
     * Safe MQ that returns -1 for unknown words instead of throwing.
     */
    public static int MQSafe(String word) {
        if (Hankel.containsKey(word)) return Hankel.get(word);
        if (positiveWords.contains(word)) { Hankel.put(word, 1); return 1; }
        if (negativeWords.contains(word)) { Hankel.put(word, 0); return 0; }
        return -1; // Unknown
    }
    
    // ==================== Learning Algorithm ====================
    
    public static void learn() throws Exception {
        // Build complete Hankel sub-table from examples
        // Strategy: Start with all examples as potential rows, find columns that have complete coverage
        
        System.out.println("  Building complete Hankel sub-table...");
        
        // Collect all words from examples as potential rows
        Set<String> potentialRows = new LinkedHashSet<>();
        potentialRows.add(""); // Always include empty string
        
        for (String word : allWords) {
            potentialRows.add(word);
            // Also add prefixes
            String[] parts = word.isEmpty() ? new String[0] : word.split(" ");
            StringBuilder prefix = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) prefix.append(" ");
                prefix.append(parts[i]);
                potentialRows.add(prefix.toString());
            }
        }
        
        // Collect all suffixes
        Set<String> potentialCols = new LinkedHashSet<>();
        potentialCols.add(""); // Always include empty string
        
        for (String letter : alphabet) {
            potentialCols.add(letter);
        }
        
        for (String word : allWords) {
            String[] parts = word.isEmpty() ? new String[0] : word.split(" ");
            for (int i = 0; i <= parts.length; i++) {
                StringBuilder suffix = new StringBuilder();
                for (int j = i; j < parts.length; j++) {
                    if (j > i) suffix.append(" ");
                    suffix.append(parts[j]);
                }
                potentialCols.add(suffix.toString());
            }
        }
        
        // Find the maximal complete sub-table
        // Start with all rows, then greedily remove rows that don't have full column coverage
        List<String> rows = new ArrayList<>(potentialRows);
        List<String> cols = new ArrayList<>(potentialCols);
        
        // Find maximal complete sub-matrix by starting with good columns
        // and keeping only rows that have full coverage for those columns
        
        // First, find columns that have known values for "most" rows
        List<String> goodCols = new ArrayList<>();
        for (String col : cols) {
            int knownCount = 0;
            for (String row : rows) {
                if (MQSafe(addStrings(row, col)) != -1) knownCount++;
            }
            // Keep columns that have at least some coverage
            if (knownCount >= rows.size() / 3 || knownCount >= 5) {
                goodCols.add(col);
            }
        }
        
        // Always ensure empty string column is included
        if (!goodCols.contains("")) goodCols.add(0, "");
        
        // Also ensure single-letter columns
        for (String letter : alphabet) {
            if (!goodCols.contains(letter)) goodCols.add(letter);
        }
        
        cols = goodCols;
        
        // Now find rows that have complete coverage for all these columns
        List<String> goodRows = new ArrayList<>();
        for (String row : rows) {
            boolean allKnown = true;
            for (String col : cols) {
                if (MQSafe(addStrings(row, col)) == -1) {
                    allKnown = false;
                    break;
                }
            }
            if (allKnown) goodRows.add(row);
        }
        
        rows = goodRows;
        
        // Ensure non-empty
        if (rows.isEmpty()) rows.add("");
        if (cols.isEmpty()) cols.add("");
        
        if (verbose) {
            System.out.println("  Complete rows: ");
            for (String r : rows) {
                String status = positiveWords.contains(r) ? "+" : (negativeWords.contains(r) ? "-" : "?");
                System.out.println("    " + (r.isEmpty() ? "ε" : r) + " [" + status + "]");
            }
        }
        
        System.out.println("  Complete sub-table: " + rows.size() + " rows x " + cols.size() + " cols");
        
        // Build the full Hankel matrix for the complete sub-table
        int[][] hankel = new int[rows.size()][cols.size()];
        for (int i = 0; i < rows.size(); i++) {
            for (int j = 0; j < cols.size(); j++) {
                int val = MQSafe(addStrings(rows.get(i), cols.get(j)));
                hankel[i][j] = (val == -1) ? 0 : val;
            }
        }
        
        // Find a maximal invertible sub-matrix using greedy approach
        // Select rows and columns that form an invertible square sub-matrix
        rowIndices = new ArrayList<>();
        List<Integer> selectedRows = new ArrayList<>();
        List<Integer> selectedCols = new ArrayList<>();
        
        // Find any starting point with non-zero diagonal
        for (int r = 0; r < rows.size() && selectedRows.isEmpty(); r++) {
            for (int c = 0; c < cols.size() && selectedRows.isEmpty(); c++) {
                if (hankel[r][c] != 0) {
                    selectedRows.add(r);
                    selectedCols.add(c);
                    rowIndices.add(rows.get(r));
                }
            }
        }
        
        if (selectedRows.isEmpty()) {
            // All zeros - use empty string as fallback
            int emptyRowIdx = rows.indexOf("");
            int emptyColIdx = cols.indexOf("");
            if (emptyRowIdx >= 0 && emptyColIdx >= 0) {
                selectedRows.add(emptyRowIdx);
                selectedCols.add(emptyColIdx);
                rowIndices.add("");
            } else {
                selectedRows.add(0);
                selectedCols.add(0);
                rowIndices.add(rows.get(0));
            }
        }
        
        // Greedily add rows and columns - try multiple combinations
        for (int iter = 0; iter < Math.min(rows.size(), cols.size()); iter++) {
            boolean added = false;
            
            // Try all pairs of (row, column) and pick the best one
            // "Best" = one where we can infer the most transition entries
            int bestR = -1, bestC = -1;
            int bestInferCount = -1;
            
            for (int r = 0; r < rows.size() && !added; r++) {
                if (selectedRows.contains(r)) continue;
                
                for (int c = 0; c < cols.size(); c++) {
                    if (selectedCols.contains(c)) continue;
                    
                    // Check if adding row r and column c keeps matrix invertible
                    List<Integer> testRows = new ArrayList<>(selectedRows);
                    List<Integer> testCols = new ArrayList<>(selectedCols);
                    testRows.add(r);
                    testCols.add(c);
                    
                    int n = testRows.size();
                    int[][] subMatrix = new int[n][n];
                    for (int i = 0; i < n; i++) {
                        for (int j = 0; j < n; j++) {
                            subMatrix[i][j] = hankel[testRows.get(i)][testCols.get(j)];
                        }
                    }
                    
                    if (isInvertibleGF2(subMatrix)) {
                        // Count how many transition entries we can compute
                        int inferCount = 0;
                        String rowStr = rows.get(r);
                        String colStr = cols.get(c);
                        for (String letter : alphabet) {
                            String extended = addStrings(rowStr, letter);
                            // Check if extended + existing cols are known
                            for (int cc : selectedCols) {
                                if (MQSafe(addStrings(extended, cols.get(cc))) != -1) {
                                    inferCount++;
                                }
                            }
                            if (MQSafe(addStrings(extended, colStr)) != -1) {
                                inferCount++;
                            }
                        }
                        
                        if (inferCount > bestInferCount) {
                            bestInferCount = inferCount;
                            bestR = r;
                            bestC = c;
                        }
                    }
                }
            }
            
            if (bestR >= 0) {
                selectedRows.add(bestR);
                selectedCols.add(bestC);
                rowIndices.add(rows.get(bestR));
                added = true;
            }
            
            if (!added) break;
        }
        
        // Set column indices to match selected columns
        colIndices = new ArrayList<>();
        for (int c : selectedCols) {
            colIndices.add(cols.get(c));
        }
        // Add remaining columns for better coverage
        for (String col : cols) {
            if (!colIndices.contains(col)) {
                colIndices.add(col);
            }
        }
        
        dimension = rowIndices.size();
        System.out.println("  Linearly independent rows: " + dimension);
        
        if (verbose) {
            displayTable();
        }
        
        // Build the M2MA
        resultFinalVector = createHypothesisFinalVector();
        resultTransitionMatrices = createHypothesisTransitionMatrices();
        
        if (resultTransitionMatrices == null) {
            System.out.println("  Warning: singular matrix");
            resultTransitionMatrices = new HashMap[alphabet.length];
            for (int i = 0; i < alphabet.length; i++) {
                resultTransitionMatrices[i] = initialize(dimension, dimension);
            }
        }
        
        // Check and refine
        String ce = findCounterexample(resultFinalVector, resultTransitionMatrices);
        if (ce == null) {
            System.out.println("  Converged with dimension " + dimension);
        } else {
            System.out.println("  Initial hypothesis has errors, trying exhaustive search...");
            
            // Try exhaustive search over unknown entries
            boolean found = exhaustiveSearchUnknowns();
            
            if (found) {
                System.out.println("  Exhaustive search succeeded with dimension " + dimension);
            } else {
                System.out.println("  Exhaustive search failed");
                System.out.println("  Counterexample: " + ce);
            }
        }
    }
    
    // Track unknown entries for exhaustive search
    public static List<int[]> unknownEntries = new ArrayList<>(); // [letterIdx, row, col]
    public static double[][][] F_letter_matrices; // Stores F_a matrices for each letter
    public static DecompositionSolver baseSolver;
    
    /**
     * Exhaustive search over unknown Hankel entries.
     * Tries all combinations of unknown values in both base matrix and transition matrices.
     */
    @SuppressWarnings("unchecked")
    public static boolean exhaustiveSearchUnknowns() throws Exception {
        // Collect unknowns in base matrix
        List<int[]> baseUnknowns = new ArrayList<>(); // [-1, i, j] for base matrix
        double[][] baseF = new double[dimension][dimension];
        
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                String word = addStrings(rowIndices.get(i), colIndices.get(j));
                int val = MQSafe(word);
                if (val == -1) val = findEquivalentMQ(rowIndices.get(i), colIndices.get(j));
                
                if (val == -1) {
                    baseUnknowns.add(new int[]{-1, i, j});
                    baseF[j][i] = 0;
                } else {
                    baseF[j][i] = val;
                }
            }
        }
        
        // Collect unknowns in transition matrices
        unknownEntries.clear();
        F_letter_matrices = new double[alphabet.length][dimension][dimension];
        
        for (int c = 0; c < alphabet.length; c++) {
            String letter = alphabet[c];
            for (int i = 0; i < dimension; i++) {
                for (int j = 0; j < dimension; j++) {
                    String word = addStrings(addStrings(rowIndices.get(i), letter), colIndices.get(j));
                    int val = MQSafe(word);
                    if (val == -1) val = findEquivalentMQ(addStrings(rowIndices.get(i), letter), colIndices.get(j));
                    
                    if (val == -1) {
                        unknownEntries.add(new int[]{c, i, j});
                        F_letter_matrices[c][i][j] = 0;
                    } else {
                        F_letter_matrices[c][i][j] = val;
                    }
                }
            }
        }
        
        int numBaseUnknowns = baseUnknowns.size();
        int numTransUnknowns = unknownEntries.size();
        int totalUnknowns = numBaseUnknowns + numTransUnknowns;
        
        System.out.println("  Found " + numBaseUnknowns + " base unknowns, " + numTransUnknowns + " transition unknowns");
        
        if (totalUnknowns == 0) {
            return false;
        }
        
        if (totalUnknowns > 20) {
            System.out.println("  Too many unknowns for exhaustive search (limit: 20)");
            return false;
        }
        
        long totalCombinations = 1L << totalUnknowns;
        System.out.println("  Searching " + totalCombinations + " combinations...");
        
        for (long combo = 0; combo < totalCombinations; combo++) {
            // Set base matrix unknowns
            double[][] testBaseF = new double[dimension][dimension];
            for (int i = 0; i < dimension; i++) {
                testBaseF[i] = baseF[i].clone();
            }
            
            for (int k = 0; k < numBaseUnknowns; k++) {
                int[] entry = baseUnknowns.get(k);
                int bit = (int)((combo >> k) & 1);
                testBaseF[entry[2]][entry[1]] = bit;
            }
            
            // Check if base matrix is invertible
            RealMatrix baseMatrix = new Array2DRowRealMatrix(testBaseF);
            DecompositionSolver testSolver;
            try {
                testSolver = new solver(baseMatrix).getSolver();
            } catch (Exception e) {
                continue; // This combination gives singular matrix
            }
            
            // Set transition matrix unknowns
            double[][][] testTransF = new double[alphabet.length][dimension][dimension];
            for (int c = 0; c < alphabet.length; c++) {
                for (int i = 0; i < dimension; i++) {
                    testTransF[c][i] = F_letter_matrices[c][i].clone();
                }
            }
            
            for (int k = 0; k < numTransUnknowns; k++) {
                int[] entry = unknownEntries.get(k);
                int bit = (int)((combo >> (numBaseUnknowns + k)) & 1);
                testTransF[entry[0]][entry[1]][entry[2]] = bit;
            }
            
            // Build transition matrices
            HashMap<Integer, ArrayList<Integer>>[] testMatrices = new HashMap[alphabet.length];
            boolean valid = true;
            
            for (int c = 0; c < alphabet.length && valid; c++) {
                testMatrices[c] = initialize(dimension, dimension);
                
                for (int i = 0; i < dimension; i++) {
                    RealVector constants = new ArrayRealVector(testTransF[c][i]);
                    try {
                        RealVector solution = testSolver.solve(constants);
                        for (int j = 0; j < dimension; j++) {
                            if (mod2(solution.getEntry(j)) == 1) {
                                addElement(testMatrices[c], i + 1, j + 1);
                            }
                        }
                    } catch (Exception e) {
                        valid = false;
                    }
                }
            }
            
            if (!valid) continue;
            
            // Test against all examples
            String ce = findCounterexample(resultFinalVector, testMatrices);
            
            if (ce == null) {
                resultTransitionMatrices = testMatrices;
                System.out.println("  Found solution at combination " + combo);
                return true;
            }
        }
        
        return false;
    }
    
    public static String findShortestPositive() {
        String shortest = null;
        int shortestLen = Integer.MAX_VALUE;
        for (String w : positiveWords) {
            int len = w.isEmpty() ? 0 : w.split(" ").length;
            if (len < shortestLen) {
                shortestLen = len;
                shortest = w;
            }
        }
        return shortest;
    }
    
    public static HashMap<Integer, ArrayList<Integer>> createHypothesisFinalVector() throws Exception {
        // Sync dimension with actual row count
        dimension = rowIndices.size();
        
        HashMap<Integer, ArrayList<Integer>> fv = initialize(1, dimension);
        for (int i = 0; i < dimension; i++) {
            int val = MQSafe(rowIndices.get(i));
            if (val == 1) {
                addElement(fv, 1, i + 1);
            }
        }
        return fv;
    }
    
    @SuppressWarnings("unchecked")
    public static HashMap<Integer, ArrayList<Integer>>[] createHypothesisTransitionMatrices() throws Exception {
        // Sync dimension with actual row count
        dimension = rowIndices.size();
        int numCols = colIndices.size();
        
        // Ensure we have at least dimension columns
        while (colIndices.size() < dimension) {
            // Add more columns from available suffixes
            for (String s : availableSuffixes) {
                if (!colIndices.contains(s)) {
                    colIndices.add(s);
                    break;
                }
            }
        }
        numCols = colIndices.size();
        
        HashMap<Integer, ArrayList<Integer>>[] matrices = new HashMap[alphabet.length];
        
        double[][] F_xi = new double[dimension][dimension];
        
        // Build the base matrix (dimension x dimension using first 'dimension' columns)
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                String word = addStrings(rowIndices.get(i), colIndices.get(j));
                int val = MQSafe(word);
                if (val == -1) {
                    // Try to find an equivalent query
                    val = findEquivalentMQ(rowIndices.get(i), colIndices.get(j));
                }
                if (val == -1) val = 0; // Default to 0 for unknown
                F_xi[j][i] = val;
            }
        }
        
        RealMatrix baseMatrix = new Array2DRowRealMatrix(F_xi);
        DecompositionSolver solver;
        try {
            solver = new solver(baseMatrix).getSolver();
        } catch (Exception e) {
            return null; // Matrix is singular
        }
        
        if (verbose) {
            System.out.println("  Base matrix F:");
            for (int i = 0; i < dimension; i++) {
                System.out.print("    ");
                for (int j = 0; j < dimension; j++) {
                    System.out.print((int)F_xi[i][j] + " ");
                }
                System.out.println();
            }
        }
        
        for (int c = 0; c < alphabet.length; c++) {
            matrices[c] = initialize(dimension, dimension);
            String letter = alphabet[c];
            
            double[][] F_xi_letter = new double[dimension][dimension];
            int unknownCount = 0;
            for (int i = 0; i < dimension; i++) {
                for (int j = 0; j < dimension; j++) {
                    String word = addStrings(addStrings(rowIndices.get(i), letter), colIndices.get(j));
                    int val = MQSafe(word);
                    if (val == -1) {
                        unknownCount++;
                        val = findEquivalentMQ(addStrings(rowIndices.get(i), letter), colIndices.get(j));
                    }
                    if (val == -1) val = 0;
                    F_xi_letter[i][j] = val;
                }
            }
            
            if (verbose && unknownCount > 0) {
                System.out.println("  Letter " + letter + ": " + unknownCount + " unknown entries");
            }
            
            if (verbose) {
                System.out.println("  F_" + letter + " matrix:");
                for (int i = 0; i < dimension; i++) {
                    System.out.print("    ");
                    for (int j = 0; j < dimension; j++) {
                        System.out.print((int)F_xi_letter[i][j] + " ");
                    }
                    System.out.println();
                }
            }
            
            for (int i = 0; i < dimension; i++) {
                RealVector constants = new ArrayRealVector(F_xi_letter[i]);
                try {
                    RealVector solution = solver.solve(constants);
                    if (verbose) {
                        System.out.print("    Row " + i + " solution: ");
                        for (int j = 0; j < dimension; j++) {
                            System.out.print(String.format("%.2f ", solution.getEntry(j)));
                        }
                        System.out.println();
                    }
                    for (int j = 0; j < dimension; j++) {
                        if (mod2(solution.getEntry(j)) == 1) {
                            addElement(matrices[c], i + 1, j + 1);
                        }
                    }
                } catch (Exception e) {
                    if (verbose) System.out.println("    Row " + i + " solver exception: " + e.getMessage());
                }
            }
        }
        
        return matrices;
    }
    
    /**
     * Try to find an equivalent MQ for prefix+suffix.
     * Uses the structure of the Hankel matrix.
     */
    public static int findEquivalentMQ(String prefix, String suffix) {
        String word = addStrings(prefix, suffix);
        
        // Try direct lookup
        int direct = MQSafe(word);
        if (direct != -1) return direct;
        
        // Strategy 1: Try different decompositions of the same word
        String[] parts = word.isEmpty() ? new String[0] : word.split(" ");
        for (int split = 0; split <= parts.length; split++) {
            StringBuilder pBuilder = new StringBuilder();
            StringBuilder sBuilder = new StringBuilder();
            for (int i = 0; i < split; i++) {
                if (i > 0) pBuilder.append(" ");
                pBuilder.append(parts[i]);
            }
            for (int i = split; i < parts.length; i++) {
                if (i > split) sBuilder.append(" ");
                sBuilder.append(parts[i]);
            }
            String p = pBuilder.toString();
            String s = sBuilder.toString();
            
            if (prefixSet.contains(p) && suffixSet.contains(s)) {
                int val = MQSafe(addStrings(p, s));
                if (val != -1) return val;
            }
        }
        
        // Strategy 2: Use linear combination of known rows
        // If prefix can be expressed as XOR of basis rows, compute the answer
        int inferred = inferFromLinearCombination(prefix, suffix);
        if (inferred != -1) return inferred;
        
        return -1;
    }
    
    /**
     * Try to infer MQ(prefix, suffix) using linear combinations of known rows.
     * 
     * If row(prefix) = row(r1) ⊕ row(r2) ⊕ ... (over columns where all are known)
     * Then MQ(prefix·suffix) = MQ(r1·suffix) ⊕ MQ(r2·suffix) ⊕ ...
     */
    public static int inferFromLinearCombination(String prefix, String suffix) {
        if (rowIndices.isEmpty() || colIndices.isEmpty()) return -1;
        
        // First, check if we can express 'prefix' as a linear combination of basis rows
        // We need to find which columns we can use for this (where prefix has known values)
        
        List<Integer> usableCols = new ArrayList<>();
        int[] prefixVals = new int[colIndices.size()];
        
        for (int j = 0; j < colIndices.size(); j++) {
            int val = MQSafe(addStrings(prefix, colIndices.get(j)));
            if (val != -1) {
                usableCols.add(j);
                prefixVals[j] = val;
            }
        }
        
        if (usableCols.size() < rowIndices.size()) {
            // Not enough known columns to determine linear combination
            return -1;
        }
        
        // Build the basis matrix for usable columns
        int n = rowIndices.size();
        int[][] basisMatrix = new int[n][usableCols.size()];
        for (int i = 0; i < n; i++) {
            for (int jIdx = 0; jIdx < usableCols.size(); jIdx++) {
                int j = usableCols.get(jIdx);
                int val = MQSafe(addStrings(rowIndices.get(i), colIndices.get(j)));
                basisMatrix[i][jIdx] = (val == -1) ? 0 : val;
            }
        }
        
        // Build the target vector (prefix row for usable columns)
        int[] targetVec = new int[usableCols.size()];
        for (int jIdx = 0; jIdx < usableCols.size(); jIdx++) {
            targetVec[jIdx] = prefixVals[usableCols.get(jIdx)];
        }
        
        // Try to express targetVec as XOR of basis rows using Gaussian elimination
        int[] coefficients = solveLinearSystemGF2(basisMatrix, targetVec);
        
        if (coefficients == null) {
            // Cannot express prefix as linear combination of basis
            return -1;
        }
        
        // Now compute MQ(prefix·suffix) = XOR of MQ(basis_i·suffix) for non-zero coefficients
        int result = 0;
        for (int i = 0; i < n; i++) {
            if (coefficients[i] == 1) {
                int val = MQSafe(addStrings(rowIndices.get(i), suffix));
                if (val == -1) {
                    // Can't compute - one of the needed values is unknown
                    return -1;
                }
                result ^= val;
            }
        }
        
        return result;
    }
    
    /**
     * Solve the system: find coefficients c such that c[0]*row[0] ⊕ c[1]*row[1] ⊕ ... = target
     * over GF(2).
     * 
     * @return array of coefficients (0 or 1), or null if no solution exists
     */
    public static int[] solveLinearSystemGF2(int[][] matrix, int[] target) {
        int numRows = matrix.length;
        if (numRows == 0) return null;
        int numCols = matrix[0].length;
        
        // Augmented matrix: [matrix^T | target]
        // We want to find which rows of 'matrix' XOR to give 'target'
        // This is equivalent to solving (matrix^T) * c = target
        
        int[][] aug = new int[numCols][numRows + 1];
        for (int j = 0; j < numCols; j++) {
            for (int i = 0; i < numRows; i++) {
                aug[j][i] = matrix[i][j];
            }
            aug[j][numRows] = target[j];
        }
        
        // Gaussian elimination
        int[] pivotCol = new int[numRows];
        Arrays.fill(pivotCol, -1);
        
        int pivotRow = 0;
        for (int col = 0; col < numRows && pivotRow < numCols; col++) {
            // Find pivot
            int pivot = -1;
            for (int row = pivotRow; row < numCols; row++) {
                if (aug[row][col] == 1) {
                    pivot = row;
                    break;
                }
            }
            
            if (pivot == -1) continue;
            
            // Swap
            int[] temp = aug[pivotRow];
            aug[pivotRow] = aug[pivot];
            aug[pivot] = temp;
            
            pivotCol[col] = pivotRow;
            
            // Eliminate
            for (int row = 0; row < numCols; row++) {
                if (row != pivotRow && aug[row][col] == 1) {
                    for (int k = 0; k <= numRows; k++) {
                        aug[row][k] ^= aug[pivotRow][k];
                    }
                }
            }
            
            pivotRow++;
        }
        
        // Check for inconsistency
        for (int row = pivotRow; row < numCols; row++) {
            if (aug[row][numRows] == 1) {
                // Inconsistent - no solution
                return null;
            }
        }
        
        // Extract solution
        int[] result = new int[numRows];
        for (int col = 0; col < numRows; col++) {
            if (pivotCol[col] != -1) {
                result[col] = aug[pivotCol[col]][numRows];
            }
        }
        
        return result;
    }
    
    /**
     * Find a counterexample from the known examples.
     */
    public static String findCounterexample(HashMap<Integer, ArrayList<Integer>> fv, 
                                            HashMap<Integer, ArrayList<Integer>>[] transitions) throws Exception {
        // Check all positive examples
        for (String word : positiveWords) {
            int result = evaluateHypothesis(fv, transitions, word);
            if (result != 1) {
                return word;
            }
        }
        
        // Check all negative examples
        for (String word : negativeWords) {
            int result = evaluateHypothesis(fv, transitions, word);
            if (result != 0) {
                return word;
            }
        }
        
        return null; // No counterexample
    }
    
    /**
     * Process a counterexample and expand the observation table.
     * Only adds rows/columns where the resulting queries can be answered.
     */
    public static boolean processCounterexample(String ce, 
                                                HashMap<Integer, ArrayList<Integer>>[] transitions) throws Exception {
        String[] parts = ce.isEmpty() ? new String[0] : ce.split(" ");
        boolean added = false;
        
        // Strategy 1: Add prefixes of the counterexample as rows (if they have enough known column values)
        for (int i = 0; i <= parts.length; i++) {
            StringBuilder wBuilder = new StringBuilder();
            for (int j = 0; j < i; j++) {
                if (j > 0) wBuilder.append(" ");
                wBuilder.append(parts[j]);
            }
            String w = wBuilder.toString();
            
            if (!rowIndices.contains(w)) {
                // Check if this row has enough known values
                int knownCount = 0;
                for (String col : colIndices) {
                    if (MQSafe(addStrings(w, col)) != -1) knownCount++;
                }
                if (knownCount >= colIndices.size() / 2 || knownCount >= 2) {
                    rowIndices.add(w);
                    added = true;
                    if (verbose) System.out.println("  Added row: " + (w.isEmpty() ? "ε" : w));
                }
            }
        }
        
        // Strategy 2: Add suffixes that create known words
        for (int i = 0; i <= parts.length; i++) {
            StringBuilder sBuilder = new StringBuilder();
            for (int j = i; j < parts.length; j++) {
                if (j > i) sBuilder.append(" ");
                sBuilder.append(parts[j]);
            }
            String suffix = sBuilder.toString();
            
            if (!colIndices.contains(suffix)) {
                // Check if this suffix creates mostly known words
                int knownCount = 0;
                for (String row : rowIndices) {
                    if (MQSafe(addStrings(row, suffix)) != -1) knownCount++;
                }
                if (knownCount >= rowIndices.size() / 2 || knownCount >= 2) {
                    colIndices.add(suffix);
                    added = true;
                    if (verbose) System.out.println("  Added col: " + (suffix.isEmpty() ? "ε" : suffix));
                }
            }
        }
        
        // Strategy 3: Add single-letter suffixes (these usually work)
        for (String letter : alphabet) {
            if (!colIndices.contains(letter)) {
                int knownCount = 0;
                for (String row : rowIndices) {
                    if (MQSafe(addStrings(row, letter)) != -1) knownCount++;
                }
                if (knownCount > 0) {
                    colIndices.add(letter);
                    added = true;
                    if (verbose) System.out.println("  Added col: " + letter);
                }
            }
        }
        
        // Update dimension
        dimension = rowIndices.size();
        
        return added;
    }
    
    public static boolean isLinearlyIndependent(String newRow) throws Exception {
        // Simplified check: see if the new row's values differ from existing rows
        int[] newRowVals = new int[colIndices.size()];
        for (int j = 0; j < colIndices.size(); j++) {
            int val = MQSafe(addStrings(newRow, colIndices.get(j)));
            newRowVals[j] = (val == -1) ? 0 : val;
        }
        
        for (int i = 0; i < dimension; i++) {
            int[] existingRow = new int[colIndices.size()];
            boolean same = true;
            for (int j = 0; j < colIndices.size(); j++) {
                int val = MQSafe(addStrings(rowIndices.get(i), colIndices.get(j)));
                existingRow[j] = (val == -1) ? 0 : val;
                if (existingRow[j] != newRowVals[j]) same = false;
            }
            if (same) return false;
        }
        return true;
    }
    
    public static boolean helpsDistinguish(String newCol) throws Exception {
        // Check if this column creates new distinctions between rows
        Set<Integer> values = new HashSet<>();
        for (int i = 0; i < dimension; i++) {
            int val = MQSafe(addStrings(rowIndices.get(i), newCol));
            values.add(val == -1 ? 0 : val);
        }
        return values.size() > 1;
    }
    
    public static boolean expandTable() throws Exception {
        // Try to add a new row with good coverage
        for (String prefix : availablePrefixes) {
            if (!rowIndices.contains(prefix)) {
                int knownCount = 0;
                for (String col : colIndices) {
                    if (MQSafe(addStrings(prefix, col)) != -1) knownCount++;
                }
                // Require at least half the columns to be known
                if (knownCount >= Math.max(1, colIndices.size() / 2)) {
                    rowIndices.add(prefix);
                    dimension = rowIndices.size();
                    if (verbose) System.out.println("  Expanded with row: " + (prefix.isEmpty() ? "ε" : prefix));
                    return true;
                }
            }
        }
        
        // Try to add a new column with good coverage
        for (String suffix : availableSuffixes) {
            if (!colIndices.contains(suffix)) {
                int knownCount = 0;
                for (String row : rowIndices) {
                    if (MQSafe(addStrings(row, suffix)) != -1) knownCount++;
                }
                if (knownCount >= Math.max(1, rowIndices.size() / 2)) {
                    colIndices.add(suffix);
                    if (verbose) System.out.println("  Expanded with col: " + (suffix.isEmpty() ? "ε" : suffix));
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Prune the table to only include rows/columns with COMPLETE coverage.
     * This ensures no unknown entries in the final table.
     */
    public static void pruneTableForCoverage() {
        boolean changed = true;
        while (changed) {
            changed = false;
            
            // Remove columns with any unknown entries
            List<String> goodCols = new ArrayList<>();
            for (String col : colIndices) {
                boolean allKnown = true;
                for (String row : rowIndices) {
                    if (MQSafe(addStrings(row, col)) == -1) {
                        allKnown = false;
                        break;
                    }
                }
                if (allKnown) {
                    goodCols.add(col);
                }
            }
            
            // Ensure we keep at least empty string column
            if (goodCols.isEmpty()) goodCols.add("");
            
            if (goodCols.size() != colIndices.size()) {
                colIndices = new ArrayList<>(goodCols);
                changed = true;
            }
            
            // Remove rows with any unknown entries (in remaining columns)
            List<String> goodRows = new ArrayList<>();
            for (String row : rowIndices) {
                boolean allKnown = true;
                for (String col : colIndices) {
                    if (MQSafe(addStrings(row, col)) == -1) {
                        allKnown = false;
                        break;
                    }
                }
                if (allKnown) {
                    goodRows.add(row);
                }
            }
            
            // Ensure we keep at least empty string row
            if (goodRows.isEmpty()) goodRows.add("");
            
            if (goodRows.size() != rowIndices.size()) {
                rowIndices = new ArrayList<>(goodRows);
                changed = true;
            }
        }
        
        dimension = rowIndices.size();
    }
    
    /**
     * Aggressive expansion when stuck - add any rows/columns that are in the examples.
     */
    public static void aggressiveExpand() {
        // Add rows from misclassified examples
        for (String word : positiveWords) {
            if (!rowIndices.contains(word)) {
                rowIndices.add(word);
                if (verbose) System.out.println("  Aggressive add row: " + word);
                dimension = rowIndices.size();
                return;
            }
            
            // Also add prefixes
            String[] parts = word.isEmpty() ? new String[0] : word.split(" ");
            StringBuilder prefix = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) prefix.append(" ");
                prefix.append(parts[i]);
                String p = prefix.toString();
                if (!rowIndices.contains(p)) {
                    rowIndices.add(p);
                    if (verbose) System.out.println("  Aggressive add row: " + p);
                    dimension = rowIndices.size();
                    return;
                }
            }
        }
    }
    
    // ==================== M2MA Evaluation ====================
    
    public static int evaluateHypothesis(HashMap<Integer, ArrayList<Integer>> fv,
                                         HashMap<Integer, ArrayList<Integer>>[] transitions,
                                         String word) throws Exception {
        int[] state = new int[dimension];
        state[0] = 1;
        
        String[] parts = word.isEmpty() ? new String[0] : word.split(" ");
        for (String letter : parts) {
            int letterIdx = letterToIndex.get(letter);
            int[] newState = new int[dimension];
            for (int i = 0; i < dimension; i++) {
                if (state[i] == 1) {
                    for (int j = 0; j < dimension; j++) {
                        if (getEntry(transitions[letterIdx], i + 1, j + 1) == 1) {
                            newState[j] ^= 1;
                        }
                    }
                }
            }
            state = newState;
        }
        
        int result = 0;
        for (int i = 0; i < dimension; i++) {
            if (state[i] == 1 && getEntry(fv, 1, i + 1) == 1) {
                result ^= 1;
            }
        }
        return result;
    }
    
    public static int evaluateM2MA(String word) throws Exception {
        return evaluateHypothesis(resultFinalVector, resultTransitionMatrices, word);
    }
    
    // ==================== Minimization ====================
    
    @SuppressWarnings("unchecked")
    public static void minimize() throws Exception {
        // First verify the hypothesis before minimization
        int wrongBefore = 0;
        for (String w : positiveWords) {
            if (evaluateM2MA(w) != 1) wrongBefore++;
        }
        for (String w : negativeWords) {
            if (evaluateM2MA(w) != 0) wrongBefore++;
        }
        
        if (wrongBefore > 0) {
            System.out.println("  Before minimization: " + wrongBefore + " errors");
            System.out.println("  Skipping minimization (hypothesis is not correct)");
            return;
        }
        
        // Use the existing M2MA minimization
        M2MA.alphabet = alphabet;
        M2MA.letterToIndex = new HashMap<>(letterToIndex);
        M2MA.inputSize = dimension;
        M2MA.inputFinalVector = resultFinalVector;
        M2MA.inputTransitionMatrices = resultTransitionMatrices;
        
        M2MA.minimize();
        
        int minDim = M2MA.minSize;
        System.out.println("  Before: " + dimension + ", After: " + minDim);
        
        // Verify minimized result
        HashMap<Integer, ArrayList<Integer>> minFV = M2MA.minFinalVector;
        HashMap<Integer, ArrayList<Integer>>[] minTM = M2MA.minTransitionMatrices;
        
        int wrongAfter = 0;
        for (String w : positiveWords) {
            if (evaluateHypothesis(minFV, minTM, w) != 1) wrongAfter++;
        }
        for (String w : negativeWords) {
            if (evaluateHypothesis(minFV, minTM, w) != 0) wrongAfter++;
        }
        
        if (wrongAfter == 0 && minDim < dimension) {
            resultFinalVector = minFV;
            resultTransitionMatrices = minTM;
            dimension = minDim;
        } else if (wrongAfter > 0) {
            System.out.println("  Minimization introduced errors, keeping original");
        }
    }
    
    // ==================== Display ====================
    
    public static void displayTable() throws Exception {
        System.out.println("  Dimension: " + dimension);
        System.out.print("  Rows: ");
        for (String r : rowIndices) System.out.print((r.isEmpty() ? "ε" : r) + " ");
        System.out.println();
        System.out.print("  Cols: ");
        for (String c : colIndices) System.out.print((c.isEmpty() ? "ε" : c) + " ");
        System.out.println();
        
        for (int i = 0; i < dimension; i++) {
            System.out.print("    ");
            for (int j = 0; j < colIndices.size(); j++) {
                int val = MQSafe(addStrings(rowIndices.get(i), colIndices.get(j)));
                System.out.print((val == -1 ? "?" : val) + " ");
            }
            System.out.println();
        }
        System.out.println();
    }
    
    public static void displayResults() {
        System.out.println("Learned M2MA");
        System.out.println("------------");
        System.out.println("Dimension: " + dimension + "\n");
        
        System.out.print("Final Vector: ");
        for (int i = 0; i < dimension; i++) {
            System.out.print(getEntry(resultFinalVector, 1, i + 1) + " ");
        }
        System.out.println("\n");
        
        System.out.println("Transition Matrices:");
        for (int a = 0; a < alphabet.length; a++) {
            System.out.println("Letter " + alphabet[a]);
            for (int i = 0; i < dimension; i++) {
                for (int j = 0; j < dimension; j++) {
                    System.out.print(getEntry(resultTransitionMatrices[a], i + 1, j + 1) + " ");
                }
                System.out.println();
            }
            System.out.println();
        }
    }
    
    public static void verify() throws Exception {
        int correctPos = 0, wrongPos = 0;
        int correctNeg = 0, wrongNeg = 0;
        
        for (String word : positiveWords) {
            if (evaluateM2MA(word) == 1) correctPos++;
            else { wrongPos++; System.out.println("  WRONG: '" + word + "' should be positive"); }
        }
        
        for (String word : negativeWords) {
            if (evaluateM2MA(word) == 0) correctNeg++;
            else { wrongNeg++; System.out.println("  WRONG: '" + word + "' should be negative"); }
        }
        
        System.out.println("Positive: " + correctPos + "/" + positiveWords.size() + " correct");
        System.out.println("Negative: " + correctNeg + "/" + negativeWords.size() + " correct");
        
        if (wrongPos == 0 && wrongNeg == 0) {
            System.out.println("\n*** SUCCESS: All examples correctly classified! ***");
        } else {
            System.out.println("\n*** WARNING: " + (wrongPos + wrongNeg) + " misclassified ***");
        }
    }
    
    // ==================== Sparse Matrix Utilities (from M2MA.java) ====================
    
    public static HashMap<Integer, ArrayList<Integer>> initialize(int rows, int cols) {
        HashMap<Integer, ArrayList<Integer>> m = new HashMap<>();
        ArrayList<Integer> dim = new ArrayList<>();
        dim.add(rows);
        dim.add(cols);
        m.put(0, dim);
        return m;
    }
    
    public static void addElement(HashMap<Integer, ArrayList<Integer>> m, int row, int col) {
        if (m.get(row) == null) m.put(row, new ArrayList<>());
        m.get(row).add(col);
        if (m.get(-col) == null) m.put(-col, new ArrayList<>());
        m.get(-col).add(row);
    }
    
    public static int getEntry(HashMap<Integer, ArrayList<Integer>> m, int row, int col) {
        if (m.get(row) == null) return 0;
        return m.get(row).contains(col) ? 1 : 0;
    }
    
    public static String addStrings(String a, String b) {
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;
        return a + " " + b;
    }
    
    public static int mod2(double n) {
        int temp = (int) Math.round(n);
        return (temp % 2 == 0) ? 0 : 1;
    }
    
    /**
     * Check if a matrix is invertible over GF(2) using Gaussian elimination.
     */
    public static boolean isInvertibleGF2(int[][] matrix) {
        int n = matrix.length;
        if (n == 0) return false;
        if (matrix[0].length != n) return false;
        
        // Make a copy
        int[][] m = new int[n][n];
        for (int i = 0; i < n; i++) {
            m[i] = matrix[i].clone();
        }
        
        // Gaussian elimination over GF(2)
        for (int col = 0; col < n; col++) {
            // Find pivot
            int pivot = -1;
            for (int row = col; row < n; row++) {
                if (m[row][col] == 1) {
                    pivot = row;
                    break;
                }
            }
            
            if (pivot == -1) {
                // No pivot found, matrix is singular
                return false;
            }
            
            // Swap rows
            if (pivot != col) {
                int[] temp = m[col];
                m[col] = m[pivot];
                m[pivot] = temp;
            }
            
            // Eliminate column
            for (int row = 0; row < n; row++) {
                if (row != col && m[row][col] == 1) {
                    for (int j = 0; j < n; j++) {
                        m[row][j] ^= m[col][j];
                    }
                }
            }
        }
        
        return true;
    }
    
    // ==================== Custom Exception ====================
    
    public static class UnknownWordException extends Exception {
        public String word;
        public UnknownWordException(String word) {
            super("Unknown word: " + word);
            this.word = word;
        }
    }
}

