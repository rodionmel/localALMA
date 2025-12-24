/*
 * CharacteristicSetGeneratingOracle.java
 * 
 * Oracle that loads a DFA from a .dot file and answers membership queries.
 * Also collects all membership queries and their answers (positive/negative).
 * Uses statistical equivalence queries (following arbitrary.java pattern).
 * 
 * Usage: 
 *   1. Call loadDFAFromDot(filename) to load the DFA
 *   2. Optionally call setEQParameters(maxTestLen, numTests) to configure EQ
 *   3. Set CharacteristicSetGeneratingOracle.active = true before learning
 * 
 * Command-line usage: Use LearnFromDFA.java wrapper program
 */

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class CharacteristicSetGeneratingOracle {
	
	// Flag to indicate if this oracle is active
	public static boolean active = false;
	
	// DFA structure
	public static Map<String, Map<String, String>> transitions = new HashMap<>(); // state -> (symbol -> nextState)
	public static Set<String> acceptingStates = new HashSet<>();
	public static String startState = null;
	public static Set<String> states = new HashSet<>();
	public static String[] alphabet = null;
	
	// Track all membership queries and their answers
	public static Map<String, Integer> allQueries = new HashMap<>(); // word -> answer (1 for accept, 0 for reject)
	public static int totalQueries = 0;
	
	// Full logging of MQ calls and EQ counterexamples
	public static List<String> mqLog = new ArrayList<>(); // Sequence of MQ calls: "word:answer"
	public static List<String> eqCounterExampleLog = new ArrayList<>(); // Sequence of counterexamples from EQ
	public static int mqCallNumber = 0;
	public static int eqCallNumber = 0;
	
	// EQ settings
	public static int EQMaxTestLen = 25;
	public static int EQNumTests = 1000;
	
	/**
	 * Load DFA from a Graphviz DOT file.
	 * Expected format:
	 *   digraph {
	 *     q0 [shape=doublecircle];  // accepting state
	 *     q1 [shape=circle];         // non-accepting state
	 *     q0 -> q1 [label="a"];
	 *     q1 -> q0 [label="b"];
	 *     // ... more transitions
	 *   }
	 */
	public static void loadDFAFromDot(String filename) throws Exception {
		transitions.clear();
		acceptingStates.clear();
		states.clear();
		allQueries.clear();
		totalQueries = 0;
		startState = null;
		
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line;
		Set<String> alphabetSet = new HashSet<>();
		
		// Pattern for state declaration: "q0 [shape=doublecircle];" or "q0 [shape=circle];"
		// Exclude "node" and "rankdir" which are graph-level attributes
		Pattern statePattern = Pattern.compile("\\s*(\\d+)\\s*\\[.*shape=(doublecircle|circle).*\\];");
		
		// Pattern for transition: "q0 -> q1 [label="a"];"
		Pattern transitionPattern = Pattern.compile("\\s*(\\w+)\\s*->\\s*(\\w+)\\s*\\[.*label=\"([^\"]+)\".*\\];");
		
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.isEmpty() || line.startsWith("//") || line.equals("digraph {") || line.equals("}")) {
				continue;
			}
			
			// Check for state declaration
			Matcher stateMatcher = statePattern.matcher(line);
			if (stateMatcher.find()) {
				String state = stateMatcher.group(1);
				String shape = stateMatcher.group(2);
				states.add(state);
				
				if (shape.equals("doublecircle")) {
					acceptingStates.add(state);
				}
				
				// First state encountered is typically the start state
				if (startState == null) {
					startState = state;
				}
				continue;
			}
			
			// Check for transition
			Matcher transMatcher = transitionPattern.matcher(line);
			if (transMatcher.find()) {
				String fromState = transMatcher.group(1);
				String toState = transMatcher.group(2);
				String symbol = transMatcher.group(3);
				
				states.add(fromState);
				states.add(toState);
				alphabetSet.add(symbol);
				
				if (!transitions.containsKey(fromState)) {
					transitions.put(fromState, new HashMap<String, String>());
				}
				transitions.get(fromState).put(symbol, toState);
				continue;
			}
		}
		
		reader.close();
		
		// Convert alphabet set to array
		alphabet = alphabetSet.toArray(new String[0]);
		Arrays.sort(alphabet);
		
		// Validate DFA
		if (startState == null) {
			throw new Exception("No start state found in DFA (no states declared)");
		}
		if (!states.contains(startState)) {
			throw new Exception("Start state " + startState + " not found in states");
		}
		
		// Validate that all states have transitions for all alphabet symbols
		// (This is a warning, not an error, as some DFAs may have incomplete transitions)
		int missingTransitions = 0;
		for (String state : states) {
			Map<String, String> stateTransitions = transitions.get(state);
			if (stateTransitions == null) {
				stateTransitions = new HashMap<>();
				transitions.put(state, stateTransitions);
			}
			for (String symbol : alphabet) {
				if (!stateTransitions.containsKey(symbol)) {
					missingTransitions++;
				}
			}
		}
		if (missingTransitions > 0) {
			System.out.println("  WARNING: DFA has " + missingTransitions + " missing transitions (words with undefined transitions will be rejected)");
		}
		
		System.out.println("CharacteristicSetGeneratingOracle: Loaded DFA from " + filename);
		System.out.println("  States: " + states.size() + " (accepting: " + acceptingStates.size() + ")");
		System.out.println("  Alphabet: " + Arrays.toString(alphabet));
		System.out.println("  Start state: " + startState);
	}
	
	/**
	 * Membership query - simulates the DFA on the input word.
	 * Also records the query and answer.
	 * 
	 * @param w The word to test (space-separated symbols, e.g., "a b a")
	 * @return 1 if accepted, 0 if rejected
	 */
	/**
	 * Simulate DFA on a word without recording the query.
	 * Used internally for EQ statistical testing.
	 * 
	 * @param w The word to test
	 * @return 1 if accepted, 0 if rejected
	 */
	private static int simulateDFA(String w) throws Exception {
		// Simulate DFA
		String currentState = startState;
		String[] symbols;
		
		if (w.isEmpty()) {
			symbols = new String[0];
		} else if (w.contains(" ")) {
			// Space-separated format: "a b c"
			symbols = w.split(" ");
		} else {
			// Concatenated format: "abc" - split into individual characters
			// Only if all characters are single-letter alphabet symbols
			symbols = new String[w.length()];
			for (int i = 0; i < w.length(); i++) {
				symbols[i] = String.valueOf(w.charAt(i));
			}
		}
		
		for (String symbol : symbols) {
			Map<String, String> stateTransitions = transitions.get(currentState);
			if (stateTransitions == null || !stateTransitions.containsKey(symbol)) {
				// No transition - reject
				return 0;
			}
			currentState = stateTransitions.get(symbol);
		}
		
		// Check if final state is accepting
		return acceptingStates.contains(currentState) ? 1 : 0;
	}
	
	/**
	 * Membership query - answers queries from the learning algorithm.
	 * Records all queries in allQueries for output to JSON.
	 * 
	 * @param w The word to test
	 * @return 1 if accepted, 0 if rejected
	 */
	public static int MQ(String w) throws Exception {
		// Check if we've already queried this word
		if (allQueries.containsKey(w)) {
			// Still log it (cached query)
			mqCallNumber++;
			mqLog.add(mqCallNumber + ": " + w + " -> " + allQueries.get(w) + " (cached)");
			return allQueries.get(w);
		}
		
		// Simulate DFA (without recording)
		int result = simulateDFA(w);
		
		// Record the query (only queries from learning algorithm)
		allQueries.put(w, result);
		totalQueries++;
		
		// Log the MQ call
		mqCallNumber++;
		mqLog.add(mqCallNumber + ": " + w + " -> " + result);
		
		return result;
	}
	
	/**
	 * Get all collected queries as positive and negative sets.
	 * Useful for analysis and characteristic set generation.
	 */
	public static void getCollectedQueries(Set<String> positiveWords, Set<String> negativeWords) {
		positiveWords.clear();
		negativeWords.clear();
		
		for (Map.Entry<String, Integer> entry : allQueries.entrySet()) {
			if (entry.getValue() == 1) {
				positiveWords.add(entry.getKey());
			} else {
				negativeWords.add(entry.getKey());
			}
		}
	}
	
	/**
	 * Get statistics about collected queries.
	 */
	public static void printQueryStatistics() {
		int positiveCount = 0;
		int negativeCount = 0;
		
		for (Integer answer : allQueries.values()) {
			if (answer == 1) {
				positiveCount++;
			} else {
				negativeCount++;
			}
		}
		
		System.out.println("CharacteristicSetGeneratingOracle Query Statistics:");
		System.out.println("  Total queries: " + totalQueries);
		System.out.println("  Unique queries: " + allQueries.size());
		System.out.println("  Positive answers: " + positiveCount);
		System.out.println("  Negative answers: " + negativeCount);
	}
	
	/**
	 * Output collected queries in JSON format matching the input example file format.
	 * Writes to the specified filename.
	 */
	public static void outputQueriesToJSON(String filename) throws Exception {
		PrintWriter writer = new PrintWriter(new FileWriter(filename));
		
		// Collect positive and negative words
		List<String> positiveWords = new ArrayList<>();
		List<String> negativeWords = new ArrayList<>();
		
		for (Map.Entry<String, Integer> entry : allQueries.entrySet()) {
			if (entry.getValue() == 1) {
				positiveWords.add(entry.getKey());
			} else {
				negativeWords.add(entry.getKey());
			}
		}
		
		// Sort for consistent output
		Collections.sort(positiveWords);
		Collections.sort(negativeWords);
		
		// Write JSON format
		writer.println("{");
		writer.println("  \"metadata\": {");
		writer.println("    \"alphabet\": [");
		for (int i = 0; i < alphabet.length; i++) {
			writer.print("      \"" + alphabet[i] + "\"");
			if (i < alphabet.length - 1) {
				writer.print(",");
			}
			writer.println();
		}
		writer.println("    ]");
		writer.println("  },");
		writer.println("  \"Positive sample\": [");
		for (int i = 0; i < positiveWords.size(); i++) {
			writer.print("    \"" + positiveWords.get(i) + "\"");
			if (i < positiveWords.size() - 1) {
				writer.print(",");
			}
			writer.println();
		}
		writer.println("  ],");
		writer.println("  \"Negative sample\": [");
		for (int i = 0; i < negativeWords.size(); i++) {
			writer.print("    \"" + negativeWords.get(i) + "\"");
			if (i < negativeWords.size() - 1) {
				writer.print(",");
			}
			writer.println();
		}
		writer.println("  ]");
		writer.println("}");
		
		writer.close();
		System.out.println("CharacteristicSetGeneratingOracle: Output " + positiveWords.size() + 
		                   " positive and " + negativeWords.size() + " negative queries to " + filename);
	}
	
	/**
	 * Equivalence query using systematic enumeration (deterministic).
	 * Tests the hypothesis against all possible test strings up to maxTestLen,
	 * in lexicographic order.
	 * Returns false with the LEXICOGRAPHICALLY SMALLEST counterexample found, true if all tests pass.
	 * 
	 * Note: Test strings are NOT added to allQueries.
	 * However, counterexamples ARE immediately recorded since they are important
	 * for learning and may not be queried via MQ() later.
	 */
	public static boolean EQ(HashMap<Integer, ArrayList<Integer>> hypothesisFinalVector, 
	                          HashMap<Integer, ArrayList<Integer>>[] hypothesisTransitionMatrices) throws Exception {
		String lexSmallestCounterExample = null;
		
		// Enumerate all possible words up to EQMaxTestLen in lexicographic order
		for (int len = 0; len <= EQMaxTestLen; len++) {
			// Generate all words of this length
			List<String> wordsOfLength = generateAllWords(len);
			
			for (String test : wordsOfLength) {
				// Use simulateDFA directly - do NOT record test queries
				int dfaAnswer = simulateDFA(test);
				int hypothesisAnswer = M2MA.MQArbitrary(hypothesisFinalVector, hypothesisTransitionMatrices, test);
				
				if (dfaAnswer != hypothesisAnswer) {
					// Found a counterexample
					// Since we iterate in lexicographic order, first found is lexicographically smallest
					lexSmallestCounterExample = test;
					
					// Record counterexample in allQueries (important for learning)
					// Check if not already recorded to avoid double-counting
					if (!allQueries.containsKey(lexSmallestCounterExample)) {
						allQueries.put(lexSmallestCounterExample, dfaAnswer);
						totalQueries++;
					}
					
					M2MA.counterExample = lexSmallestCounterExample;
					
					// Log the counterexample
					eqCallNumber++;
					eqCounterExampleLog.add(eqCallNumber + ": " + lexSmallestCounterExample);
					
					return false;
				}
			}
		}
		
		// All tests passed - log that no counterexample was found
		eqCallNumber++;
		eqCounterExampleLog.add(eqCallNumber + ": (no counterexample - hypothesis correct)");
		
		return true;
	}
	
	/**
	 * Generate all possible words of a given length in lexicographic order.
	 * Words are space-separated (e.g., "a b c").
	 * 
	 * @param length The length of words to generate
	 * @return List of all words of given length, in lexicographic order
	 */
	private static List<String> generateAllWords(int length) {
		List<String> words = new ArrayList<>();
		
		if (length == 0) {
			words.add("");
			return words;
		}
		
		// Generate all combinations recursively
		generateWordsRecursive("", length, words);
		
		return words;
	}
	
	/**
	 * Recursively generate all words of given length.
	 * 
	 * @param prefix Current prefix
	 * @param remaining Remaining length
	 * @param words List to add words to
	 */
	private static void generateWordsRecursive(String prefix, int remaining, List<String> words) {
		if (remaining == 0) {
			words.add(prefix.trim());
			return;
		}
		
		// Try each symbol in alphabet order (for lexicographic ordering)
		for (String symbol : alphabet) {
			String newPrefix = prefix.isEmpty() ? symbol : prefix + " " + symbol;
			generateWordsRecursive(newPrefix, remaining - 1, words);
		}
	}
	
	/**
	 * Set EQ parameters.
	 * Should be called before learning starts.
	 */
	public static void setEQParameters(int maxTestLen, int numTests) {
		EQMaxTestLen = maxTestLen;
		EQNumTests = numTests;
	}
	
	/**
	 * Reset the oracle state for a new learning session.
	 * Note: This does NOT clear the DFA or collected queries.
	 */
	public static void reset() {
		// No state to reset for simplified EQ
	}
	
	/**
	 * Clear collected queries (but keep DFA).
	 */
	public static void clearQueries() {
		allQueries.clear();
		totalQueries = 0;
		mqLog.clear();
		eqCounterExampleLog.clear();
		mqCallNumber = 0;
		eqCallNumber = 0;
	}
}

