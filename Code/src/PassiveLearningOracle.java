/*
 * PassiveLearningOracle.java
 * 
 * Passive learning oracle that learns from a fixed set of examples.
 * Unlike active learning, this oracle only uses the provided examples
 * and does not generate new queries.
 * 
 * Usage: Set PassiveLearningOracle.active = true and load examples before learning.
 * 
 * Command-line usage: Use LearnFromPassiveOracle.java wrapper program
 */

import java.io.*;
import java.util.*;

public class PassiveLearningOracle {
	
	// Flag to indicate if this oracle is active
	public static boolean active = false;
	
	// Storage for examples
	public static Set<String> positiveWords = new HashSet<>();
	public static Set<String> negativeWords = new HashSet<>();
	public static Set<String> allWords = new HashSet<>();
	
	// Closed-world assumption: unknown words are assumed negative
	public static boolean closedWorld = false;
	
	// Full logging of MQ calls and EQ counterexamples
	public static List<String> mqLog = new ArrayList<>(); // Sequence of MQ calls: "word:answer"
	public static List<String> eqCounterExampleLog = new ArrayList<>(); // Sequence of counterexamples from EQ
	public static int mqCallNumber = 0;
	public static int eqCallNumber = 0;
	
	/**
	 * Load examples from a JSON file.
	 * Expected format:
	 * {
	 *   "metadata": { "alphabet": ["a", "b"], ... },
	 *   "Positive sample": ["", "a a", ...],
	 *   "Negative sample": ["a", "b", ...]
	 * }
	 */
	public static String[] loadFromJSON(String filename) throws Exception {
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
			String[] parts = alphaArray.split(",");
			for (String part : parts) {
				String letter = part.trim().replace("\"", "").trim();
				if (!letter.isEmpty()) {
					alphabetList.add(letter);
				}
			}
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
		
		allWords.clear();
		allWords.addAll(positiveWords);
		allWords.addAll(negativeWords);
		
		System.out.println("PassiveLearningOracle: Loaded " + positiveWords.size() + 
		                   " positive and " + negativeWords.size() + " negative examples");
		
		return alphabetList.toArray(new String[0]);
	}
	
	/**
	 * Membership query for passive learning.
	 * Returns 1 if word is in positiveWords, 0 if in negativeWords,
	 * and uses closed-world assumption for unknown words.
	 */
	public static int MQ(String w) {
		// Always fail if word is not in the example set
		if (!allWords.contains(w)) {
			throw new RuntimeException("Unknown word encountered in passive learning: " + w + " (not in example set)");
		}
		
		int result;
		if (positiveWords.contains(w)) {
			result = 1;
		} else if (negativeWords.contains(w)) {
			result = 0;
		} else {
			// Should not happen since we check allWords first
			throw new RuntimeException("Word in allWords but not in positive or negative: " + w);
		}
		
		// Log the MQ call
		mqCallNumber++;
		mqLog.add(mqCallNumber + ": " + w + " -> " + result);
		
		return result;
	}
	
	/**
	 * Equivalence query for passive learning.
	 * Checks that the hypothesis correctly classifies all examples:
	 * - All positive words must be accepted (hypothesis returns 1)
	 * - All negative words must be rejected (hypothesis returns 0)
	 * 
	 * Returns true if hypothesis matches all examples, false otherwise.
	 * If false, sets M2MA.counterExample to the LEXICOGRAPHICALLY SMALLEST mismatched word.
	 */
	public static boolean EQ(HashMap<Integer, ArrayList<Integer>> hypothesisFinalVector, 
	                          HashMap<Integer, ArrayList<Integer>>[] hypothesisTransitionMatrices) throws Exception {
		List<String> counterExamples = new ArrayList<>();
		
		// Check all positive words - they must be accepted (hypothesis returns 1)
		for (String word : positiveWords) {
			int expectedValue = 1; // Positive words should be accepted
			int hypothesisValue = M2MA.MQArbitrary(hypothesisFinalVector, hypothesisTransitionMatrices, word);
			
			if (hypothesisValue != expectedValue) {
				// Found a counterexample: positive word was rejected
				counterExamples.add(word);
			}
		}
		
		// Check all negative words - they must be rejected (hypothesis returns 0)
		for (String word : negativeWords) {
			int expectedValue = 0; // Negative words should be rejected
			int hypothesisValue = M2MA.MQArbitrary(hypothesisFinalVector, hypothesisTransitionMatrices, word);
			
			if (hypothesisValue != expectedValue) {
				// Found a counterexample: negative word was accepted
				counterExamples.add(word);
			}
		}
		
		// If we found any counterexamples, find lexicographically smallest
		if (!counterExamples.isEmpty()) {
			// Sort to find lexicographically smallest
			Collections.sort(counterExamples);
			String lexSmallestCounterExample = counterExamples.get(0);
			
			M2MA.counterExample = lexSmallestCounterExample;
			
			// Log the counterexample
			eqCallNumber++;
			eqCounterExampleLog.add(eqCallNumber + ": " + lexSmallestCounterExample);
			
			return false;
		}
		
		// All examples match - hypothesis is correct
		// Log that no counterexample was found
		eqCallNumber++;
		eqCounterExampleLog.add(eqCallNumber + ": (no counterexample - hypothesis correct)");
		
		return true;
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
		StringBuilder current = new StringBuilder();
		boolean inQuote = false;
		boolean foundQuote = false;
		
		for (int i = 0; i < arrayContent.length(); i++) {
			char c = arrayContent.charAt(i);
			if (c == '"') {
				inQuote = !inQuote;
				foundQuote = true;
			} else if (c == ',' && !inQuote) {
				if (foundQuote) {
					target.add(current.toString());
				}
				current = new StringBuilder();
				foundQuote = false;
			} else if (inQuote) {
				current.append(c);
			}
		}
		if (foundQuote) {
			target.add(current.toString());
		}
	}
}

