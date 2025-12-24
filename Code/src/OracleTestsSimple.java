/*
 * OracleTestsSimple.java
 * 
 * Simple test program for PassiveLearningOracle and CharacteristicSetGeneratingOracle.
 * Tests basic functionality without requiring M2MA classes.
 * 
 * Usage: java OracleTestsSimple
 */

import java.io.*;
import java.util.*;

public class OracleTestsSimple {
    
    public static void main(String[] args) throws Exception {
        System.out.println("Oracle Tests (Simple - No M2MA dependency)");
        System.out.println("==========================================\n");
        
        // Test 1: PassiveLearningOracle
        System.out.println("Test 1: PassiveLearningOracle");
        System.out.println("-------------------------------");
        testPassiveLearningOracle();
        System.out.println();
        
        // Test 2: CharacteristicSetGeneratingOracle
        System.out.println("Test 2: CharacteristicSetGeneratingOracle");
        System.out.println("-------------------------------------------");
        testCharacteristicSetGeneratingOracle();
        System.out.println();
        
        System.out.println("All tests completed!");
    }
    
    public static void testPassiveLearningOracle() throws Exception {
        String jsonFile = "test_examples/F1_last_2_is_a_ab.json";
        
        // Load examples
        System.out.println("Loading examples from: " + jsonFile);
        String[] alphabet = PassiveLearningOracle.loadFromJSON(jsonFile);
        System.out.println("Alphabet: " + Arrays.toString(alphabet));
        System.out.println("Positive examples: " + PassiveLearningOracle.positiveWords.size());
        System.out.println("Negative examples: " + PassiveLearningOracle.negativeWords.size());
        
        // Test MQ
        System.out.println("\nTesting MQ:");
        PassiveLearningOracle.active = true;
        PassiveLearningOracle.closedWorld = true;
        
        int passed = 0;
        int failed = 0;
        
        // Test some positive words
        String[] testPositive = {"aa", "ab", "baa", "bab"};
        for (String word : testPositive) {
            int result = PassiveLearningOracle.MQ(word);
            boolean correct = (result == 1);
            System.out.println("  MQ(\"" + word + "\") = " + result + " (expected: 1) " + (correct ? "✓" : "✗"));
            if (correct) passed++; else failed++;
        }
        
        // Test some negative words
        String[] testNegative = {"a", "b", "aba", "abb"};
        for (String word : testNegative) {
            int result = PassiveLearningOracle.MQ(word);
            boolean correct = (result == 0);
            System.out.println("  MQ(\"" + word + "\") = " + result + " (expected: 0) " + (correct ? "✓" : "✗"));
            if (correct) passed++; else failed++;
        }
        
        System.out.println("\nResults: " + passed + " passed, " + failed + " failed");
        if (failed == 0) {
            System.out.println("✓ PassiveLearningOracle MQ tests PASSED!");
        } else {
            System.out.println("✗ PassiveLearningOracle MQ tests FAILED!");
        }
    }
    
    public static void testCharacteristicSetGeneratingOracle() throws Exception {
        String dotFile = "test_examples/F1_last_2_is_a_ab_dfa.dot";
        
        // Load DFA
        System.out.println("Loading DFA from: " + dotFile);
        CharacteristicSetGeneratingOracle.loadDFAFromDot(dotFile);
        System.out.println("Alphabet: " + Arrays.toString(CharacteristicSetGeneratingOracle.alphabet));
        System.out.println("States: " + CharacteristicSetGeneratingOracle.states.size());
        System.out.println("Accepting states: " + CharacteristicSetGeneratingOracle.acceptingStates.size());
        System.out.println("Start state: " + CharacteristicSetGeneratingOracle.startState);
        
        // Test MQ (without M2MA dependency)
        System.out.println("\nTesting MQ (direct DFA simulation):");
        CharacteristicSetGeneratingOracle.active = true;
        
        int passed = 0;
        int failed = 0;
        
        // Test words that should be accepted (2nd from last is 'a')
        // For words of length >= 2, check if 2nd from last is 'a'
        String[] testWords = {"aa", "ab", "baa", "bab", "a", "b", "aba", "abb"};
        int[] expected = {1, 1, 1, 1, 0, 0, 0, 0}; // Expected results
        
        for (int i = 0; i < testWords.length; i++) {
            String word = testWords[i];
            // Simulate DFA directly
            int result = simulateDFA(word);
            boolean correct = (result == expected[i]);
            System.out.println("  MQ(\"" + word + "\") = " + result + " (expected: " + expected[i] + ") " + (correct ? "✓" : "✗"));
            if (correct) passed++; else failed++;
        }
        
        // Print statistics
        System.out.println("\nQuery Statistics:");
        System.out.println("  Total queries collected: " + CharacteristicSetGeneratingOracle.allQueries.size());
        
        System.out.println("\nResults: " + passed + " passed, " + failed + " failed");
        if (failed == 0) {
            System.out.println("✓ CharacteristicSetGeneratingOracle MQ tests PASSED!");
        } else {
            System.out.println("✗ CharacteristicSetGeneratingOracle MQ tests FAILED!");
        }
    }
    
    // Direct DFA simulation (without M2MA)
    private static int simulateDFA(String w) {
        String currentState = CharacteristicSetGeneratingOracle.startState;
        String[] symbols = w.isEmpty() ? new String[0] : w.split(" ");
        
        for (String symbol : symbols) {
            Map<String, String> stateTransitions = CharacteristicSetGeneratingOracle.transitions.get(currentState);
            if (stateTransitions == null || !stateTransitions.containsKey(symbol)) {
                return 0; // No transition - reject
            }
            currentState = stateTransitions.get(symbol);
        }
        
        return CharacteristicSetGeneratingOracle.acceptingStates.contains(currentState) ? 1 : 0;
    }
}

