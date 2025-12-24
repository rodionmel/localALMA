/*
 * VerifyEquivalence.java
 * 
 * Verifies that a learned M2MA is equivalent to the original DFA.
 * 
 * Usage: java VerifyEquivalence <dfa.dot>
 * 
 * Note: This assumes M2MA.resultFinalVector and M2MA.resultTransitionMatrices
 *       are set from a previous learning run.
 */

import java.io.*;
import java.util.*;

public class VerifyEquivalence {
    
    public static void main(String[] args) throws Exception {
        System.out.println("Verify Equivalence: DFA vs Learned M2MA");
        System.out.println("=========================================\n");
        
        if (args.length < 1) {
            System.out.println("Usage: java VerifyEquivalence <dfa.dot>");
            System.out.println("  dfa.dot : Original DFA file");
            System.exit(1);
        }
        
        String dotFile = args[0];
        
        // Load original DFA
        System.out.println("Loading original DFA from: " + dotFile);
        CharacteristicSetGeneratingOracle.loadDFAFromDot(dotFile);
        CharacteristicSetGeneratingOracle.active = true;
        System.out.println();
        
        // Check if M2MA was learned
        if (M2MA.resultFinalVector == null || M2MA.resultTransitionMatrices == null) {
            System.out.println("ERROR: No learned M2MA found.");
            System.out.println("Please run learning algorithm first.");
            System.exit(1);
        }
        
        System.out.println("Learned M2MA dimension: " + M2MA.learnedSize);
        System.out.println();
        
        // Test on collected queries (if any)
        if (CharacteristicSetGeneratingOracle.allQueries.size() > 0) {
            System.out.println("Testing on collected queries (" + 
                             CharacteristicSetGeneratingOracle.allQueries.size() + " words)...");
            testQueries(M2MA.resultFinalVector, M2MA.resultTransitionMatrices);
        }
        
        // Test on random words
        System.out.println("\nTesting on random words (1000 words, max length 15)...");
        testRandomWords(M2MA.resultFinalVector, M2MA.resultTransitionMatrices, 1000, 15);
        
        System.out.println("\nVerification completed!");
    }
    
    /**
     * Test on collected queries.
     */
    public static void testQueries(HashMap<Integer, ArrayList<Integer>> finalVector,
                                   HashMap<Integer, ArrayList<Integer>>[] transitionMatrices) throws Exception {
        int correct = 0;
        int wrong = 0;
        List<String> mismatches = new ArrayList<>();
        
        for (Map.Entry<String, Integer> entry : CharacteristicSetGeneratingOracle.allQueries.entrySet()) {
            String word = entry.getKey();
            int dfaAnswer = CharacteristicSetGeneratingOracle.MQ(word);
            int m2maAnswer = M2MA.MQArbitrary(finalVector, transitionMatrices, word);
            
            if (dfaAnswer == m2maAnswer) {
                correct++;
            } else {
                wrong++;
                if (mismatches.size() < 10) {
                    mismatches.add(word);
                }
            }
        }
        
        System.out.println("  Correct: " + correct + "/" + (correct + wrong));
        System.out.println("  Wrong: " + wrong);
        
        if (wrong > 0 && mismatches.size() > 0) {
            System.out.println("  First " + Math.min(mismatches.size(), 10) + " mismatches:");
            for (String word : mismatches) {
                int dfaAnswer = CharacteristicSetGeneratingOracle.MQ(word);
                int m2maAnswer = M2MA.MQArbitrary(finalVector, transitionMatrices, word);
                System.out.println("    \"" + word + "\": DFA=" + dfaAnswer + ", M2MA=" + m2maAnswer);
            }
        }
    }
    
    /**
     * Test on random words.
     */
    public static void testRandomWords(HashMap<Integer, ArrayList<Integer>> finalVector,
                                      HashMap<Integer, ArrayList<Integer>>[] transitionMatrices,
                                      int numTests, int maxLen) throws Exception {
        int correct = 0;
        int wrong = 0;
        List<String> mismatches = new ArrayList<>();
        
        for (int i = 0; i < numTests; i++) {
            String test = M2MA.genTest((int) (Math.random() * (maxLen + 1)), false);
            int dfaAnswer = CharacteristicSetGeneratingOracle.MQ(test);
            int m2maAnswer = M2MA.MQArbitrary(finalVector, transitionMatrices, test);
            
            if (dfaAnswer == m2maAnswer) {
                correct++;
            } else {
                wrong++;
                if (mismatches.size() < 10) {
                    mismatches.add(test);
                }
            }
        }
        
        System.out.println("  Correct: " + correct + "/" + (correct + wrong));
        System.out.println("  Wrong: " + wrong);
        System.out.println("  Success rate: " + String.format("%.2f", 100.0 * correct / (correct + wrong)) + "%");
        
        if (wrong > 0 && mismatches.size() > 0) {
            System.out.println("  First " + Math.min(mismatches.size(), 10) + " mismatches:");
            for (String word : mismatches) {
                int dfaAnswer = CharacteristicSetGeneratingOracle.MQ(word);
                int m2maAnswer = M2MA.MQArbitrary(finalVector, transitionMatrices, word);
                System.out.println("    \"" + word + "\": DFA=" + dfaAnswer + ", M2MA=" + m2maAnswer);
            }
        }
        
        if (wrong == 0) {
            System.out.println("\n  ✓ Perfect equivalence!");
        } else {
            System.out.println("\n  ✗ " + wrong + " mismatches found");
        }
    }
}

