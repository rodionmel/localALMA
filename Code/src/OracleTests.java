/*
 * OracleTests.java
 * 
 * Simple test program for PassiveLearningOracle and CharacteristicSetGeneratingOracle.
 * Tests both oracles with small example files.
 * 
 * Usage: java OracleTests
 */

import java.io.*;
import java.util.*;

public class OracleTests {
    
    public static void main(String[] args) throws Exception {
        System.out.println("Oracle Tests");
        System.out.println("============\n");
        
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
        
        // Test some positive words
        String[] testPositive = {"aa", "ab", "baa", "bab"};
        for (String word : testPositive) {
            int result = PassiveLearningOracle.MQ(word);
            System.out.println("  MQ(\"" + word + "\") = " + result + " (expected: 1)");
            if (result != 1) {
                System.out.println("    ERROR: Expected 1!");
            }
        }
        
        // Test some negative words
        String[] testNegative = {"a", "b", "aba", "abb"};
        for (String word : testNegative) {
            int result = PassiveLearningOracle.MQ(word);
            System.out.println("  MQ(\"" + word + "\") = " + result + " (expected: 0)");
            if (result != 0) {
                System.out.println("    ERROR: Expected 0!");
            }
        }
        
        System.out.println("PassiveLearningOracle MQ tests passed!");
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
        
        // Test MQ
        System.out.println("\nTesting MQ:");
        CharacteristicSetGeneratingOracle.active = true;
        
        // Test some words that should be accepted (based on the language: 2nd from last is 'a')
        String[] testPositive = {"aa", "ab", "baa", "bab"};
        for (String word : testPositive) {
            int result = CharacteristicSetGeneratingOracle.MQ(word);
            System.out.println("  MQ(\"" + word + "\") = " + result);
        }
        
        // Test some words that should be rejected
        String[] testNegative = {"a", "b", "aba", "abb"};
        for (String word : testNegative) {
            int result = CharacteristicSetGeneratingOracle.MQ(word);
            System.out.println("  MQ(\"" + word + "\") = " + result);
        }
        
        // Print statistics
        System.out.println("\nQuery Statistics:");
        CharacteristicSetGeneratingOracle.printQueryStatistics();
        
        System.out.println("CharacteristicSetGeneratingOracle MQ tests passed!");
    }
}





