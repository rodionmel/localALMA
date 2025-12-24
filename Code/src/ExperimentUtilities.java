/*
 * ExperimentUtilities.java
 * 
 * Utility functions for experiment variations:
 * - Random reordering of samples
 * - Adding redundant examples
 */

import java.io.*;
import java.util.*;

public class ExperimentUtilities {
    
    /**
     * Shuffle the positive and negative samples in a JSON file.
     * Creates a new file with shuffled samples.
     * 
     * @param inputJsonFile  Input JSON file
     * @param outputJsonFile Output JSON file (can be same as input)
     */
    public static void shuffleSamples(String inputJsonFile, String outputJsonFile) throws Exception {
        System.out.println("Shuffling samples in JSON file...");
        
        // Load the JSON file
        String[] alphabet = PassiveLearningOracle.loadFromJSON(inputJsonFile);
        List<String> positive = new ArrayList<>(PassiveLearningOracle.positiveWords);
        List<String> negative = new ArrayList<>(PassiveLearningOracle.negativeWords);
        
        // Shuffle
        Collections.shuffle(positive, new Random());
        Collections.shuffle(negative, new Random());
        
        System.out.println("  Shuffled " + positive.size() + " positive and " + negative.size() + " negative examples");
        
        // Write back to JSON
        writeJSON(outputJsonFile, alphabet, positive, negative);
        
        System.out.println("  Written shuffled samples to: " + outputJsonFile);
    }
    
    /**
     * Add redundant examples to a JSON file based on DFA acceptance.
     * Generates random strings and tests them against the DFA.
     * 
     * @param inputJsonFile  Input JSON file
     * @param outputJsonFile Output JSON file
     * @param dfaFile        DFA file to test against
     * @param numAdditional  Number of additional examples to add
     * @param minLength      Minimum length for additional examples
     * @param maxLength      Maximum length for additional examples
     */
    public static void addRedundantExamples(String inputJsonFile, String outputJsonFile,
                                           String dfaFile, int numAdditional,
                                           int minLength, int maxLength) throws Exception {
        System.out.println("Adding redundant examples...");
        System.out.println("  Target: " + numAdditional + " additional examples");
        System.out.println("  Length range: " + minLength + "-" + maxLength);
        
        // Load original JSON
        String[] alphabet = PassiveLearningOracle.loadFromJSON(inputJsonFile);
        Set<String> existingPositive = new HashSet<>(PassiveLearningOracle.positiveWords);
        Set<String> existingNegative = new HashSet<>(PassiveLearningOracle.negativeWords);
        
        // Load DFA
        CharacteristicSetGeneratingOracle.loadDFAFromDot(dfaFile);
        CharacteristicSetGeneratingOracle.active = true;
        
        // Generate additional examples
        List<String> newPositive = new ArrayList<>();
        List<String> newNegative = new ArrayList<>();
        Random random = new Random();
        int attempts = 0;
        int maxAttempts = numAdditional * 10; // Limit attempts to avoid infinite loop
        
        while ((newPositive.size() + newNegative.size() < numAdditional) && attempts < maxAttempts) {
            attempts++;
            
            // Generate random word
            int length = minLength + random.nextInt(maxLength - minLength + 1);
            String word = generateRandomWord(alphabet, length);
            
            // Skip if already exists
            if (existingPositive.contains(word) || existingNegative.contains(word) ||
                newPositive.contains(word) || newNegative.contains(word)) {
                continue;
            }
            
            // Test against DFA
            int result = CharacteristicSetGeneratingOracle.MQ(word);
            
            if (result == 1) {
                newPositive.add(word);
            } else {
                newNegative.add(word);
            }
        }
        
        // Combine with original
        List<String> allPositive = new ArrayList<>(PassiveLearningOracle.positiveWords);
        allPositive.addAll(newPositive);
        
        List<String> allNegative = new ArrayList<>(PassiveLearningOracle.negativeWords);
        allNegative.addAll(newNegative);
        
        System.out.println("  Added " + newPositive.size() + " positive and " + 
                         newNegative.size() + " negative examples");
        System.out.println("  Total: " + allPositive.size() + " positive, " + 
                         allNegative.size() + " negative");
        
        // Write to JSON
        writeJSON(outputJsonFile, alphabet, allPositive, allNegative);
        
        System.out.println("  Written to: " + outputJsonFile);
    }
    
    /**
     * Generate a random word from the alphabet.
     * Uses space-separated format to match M2MA.genTest().
     */
    private static String generateRandomWord(String[] alphabet, int length) {
        if (length == 0) {
            return "";
        }
        
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(alphabet[random.nextInt(alphabet.length)]);
        }
        
        return sb.toString();
    }
    
    /**
     * Write examples to JSON file in the standard format.
     */
    private static void writeJSON(String filename, String[] alphabet,
                                  List<String> positive, List<String> negative) throws Exception {
        PrintWriter writer = new PrintWriter(new FileWriter(filename));
        
        // Sort for consistent output
        Collections.sort(positive);
        Collections.sort(negative);
        
        // Write JSON format
        writer.println("{");
        writer.println("  \"metadata\": {");
        writer.println("    \"alphabet\": [");
        List<String> sortedAlphabet = Arrays.asList(alphabet);
        Collections.sort(sortedAlphabet);
        for (int i = 0; i < sortedAlphabet.size(); i++) {
            writer.println("      \"" + sortedAlphabet.get(i) + "\"" + 
                         (i < sortedAlphabet.size() - 1 ? "," : ""));
        }
        writer.println("    ]");
        writer.println("  },");
        writer.println("  \"Positive sample\": [");
        for (int i = 0; i < positive.size(); i++) {
            writer.println("    \"" + escapeJsonString(positive.get(i)) + "\"" + 
                         (i < positive.size() - 1 ? "," : ""));
        }
        writer.println("  ],");
        writer.println("  \"Negative sample\": [");
        for (int i = 0; i < negative.size(); i++) {
            writer.println("    \"" + escapeJsonString(negative.get(i)) + "\"" + 
                         (i < negative.size() - 1 ? "," : ""));
        }
        writer.println("  ]");
        writer.println("}");
        writer.close();
    }
    
    /**
     * Escape special characters in JSON strings.
     */
    private static String escapeJsonString(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

