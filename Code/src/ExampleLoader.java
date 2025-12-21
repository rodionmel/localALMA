/*
 * Author: Nevin George
 * Advisor: Dana Angluin
 * Program Description: Handles loading examples from JSON files and provides
 * membership query functionality based on loaded examples.
 */

import java.io.*;
import java.util.*;

public class ExampleLoader {
	
	// Storage for examples loaded from JSON
	public static Set<String> positiveWords = new HashSet<>();
	public static Set<String> negativeWords = new HashSet<>();
	public static boolean closedWorld = true; // If true, unknown words are assumed negative
	
	/**
	 * MQ from loaded examples (closed-world assumption).
	 * Returns 1 if word is in positiveWords, 0 otherwise.
	 */
	public static int MQExamples(String w) {
		if (positiveWords.contains(w)) return 1;
		if (negativeWords.contains(w)) return 0;
		// Unknown word - use closed-world assumption
		return closedWorld ? 0 : 0;
	}
	
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
		
		System.out.println("Loaded " + positiveWords.size() + " positive and " + 
		                   negativeWords.size() + " negative examples");
		
		return alphabetList.toArray(new String[0]);
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

