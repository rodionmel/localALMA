/*
 * DFAToPassiveLearningExperiment.java
 * 
 * Experiment to test if queries collected from CharacteristicSetGeneratingOracle
 * are sufficient to learn the same M2MA using PassiveLearningOracle.
 * 
 * Usage: java DFAToPassiveLearningExperiment <dfa.dot> [maxTestLen] [numTests]
 */

import java.io.*;
import java.util.*;

public class DFAToPassiveLearningExperiment {
    
    public static void main(String[] args) throws Exception {
        System.out.println("DFA to Passive Learning Experiment");
        System.out.println("==================================\n");
        System.out.println("Hypothesis: Queries collected from CharacteristicSetGeneratingOracle");
        System.out.println("           are sufficient to learn equivalent M2MA with PassiveLearningOracle\n");
        
        if (args.length < 1) {
            System.out.println("Usage: java DFAToPassiveLearningExperiment <dfa.dot> [maxTestLen] [numTests] [applyVariations] [numAdditional] [minLength] [maxLength]");
            System.out.println("  dfa.dot         : Graphviz DOT file with DFA definition");
            System.out.println("  maxTestLen      : max length for EQ test strings (default: 25)");
            System.out.println("  numTests        : number of EQ test strings (default: 1000)");
            System.out.println("  applyVariations : true/false - apply shuffling and redundancy (default: false)");
            System.out.println("  numAdditional   : number of redundant examples to add (default: 50, only if applyVariations=true)");
            System.out.println("  minLength       : min length for redundant examples (default: 3, only if applyVariations=true)");
            System.out.println("  maxLength       : max length for redundant examples (default: 8, only if applyVariations=true)");
            System.exit(1);
        }
        
        String dotFile = args[0];
        int maxTestLen = args.length > 1 ? Integer.parseInt(args[1]) : 25;
        int numTests = args.length > 2 ? Integer.parseInt(args[2]) : 1000;
        
        // ========================================
        // PHASE 1: Learn with CharacteristicSetGeneratingOracle
        // ========================================
        System.out.println("PHASE 1: Learning with CharacteristicSetGeneratingOracle");
        System.out.println("==========================================================\n");
        
        // Load DFA
        System.out.println("Loading DFA from: " + dotFile);
        CharacteristicSetGeneratingOracle.loadDFAFromDot(dotFile);
        System.out.println();
        
        // Configure EQ parameters
        CharacteristicSetGeneratingOracle.setEQParameters(maxTestLen, numTests);
        System.out.println("EQ settings: maxTestLen=" + maxTestLen + ", numTests=" + numTests);
        System.out.println();
        
        // Activate oracle
        CharacteristicSetGeneratingOracle.active = true;
        PassiveLearningOracle.active = false; // Ensure only one is active
        
        // Set up alphabet in M2MA
        M2MA.alphabet = CharacteristicSetGeneratingOracle.alphabet;
        M2MA.letterToIndex = new HashMap<>();
        for (int i = 0; i < M2MA.alphabet.length; i++) {
            M2MA.letterToIndex.put(M2MA.alphabet[i], i);
        }
        M2MA.Hankel = new HashMap<>();
        M2MA.startTime = System.nanoTime();
        
        // Run learning
        System.out.println("Starting learning process...");
        System.out.println("----------------------------\n");
        M2MA.learn();
        
        // Save Phase 1 results
        HashMap<Integer, ArrayList<Integer>> phase1FinalVector = M2MA.resultFinalVector;
        HashMap<Integer, ArrayList<Integer>>[] phase1TransitionMatrices = M2MA.resultTransitionMatrices;
        int phase1Dimension = M2MA.learnedSize;
        
        System.out.println("\nPhase 1 Results:");
        System.out.println("  Learned dimension: " + phase1Dimension);
        M2MA.displayRuntime();
        
        // Print query statistics
        System.out.println();
        CharacteristicSetGeneratingOracle.printQueryStatistics();
        
        // Output queries to JSON
        String queriesFile = dotFile.replace(".dot", "_queries.json");
        if (queriesFile.equals(dotFile)) {
            queriesFile = dotFile + "_queries.json";
        }
        System.out.println();
        CharacteristicSetGeneratingOracle.outputQueriesToJSON(queriesFile);
        
        // ========================================
        // OPTIONAL: Apply experiment variations
        // ========================================
        String phase2InputFile = queriesFile;
        boolean applyVariations = args.length > 3 && Boolean.parseBoolean(args[3]);
        
        if (applyVariations) {
            System.out.println("\n\nAPPLYING EXPERIMENT VARIATIONS");
            System.out.println("================================\n");
            
            // Variation 1: Shuffle samples
            String shuffledFile = queriesFile.replace(".json", "_shuffled.json");
            ExperimentUtilities.shuffleSamples(queriesFile, shuffledFile);
            phase2InputFile = shuffledFile;
            
            // Variation 2: Add redundant examples
            int numAdditional = args.length > 4 ? Integer.parseInt(args[4]) : 50;
            int minLength = args.length > 5 ? Integer.parseInt(args[5]) : 3;
            int maxLength = args.length > 6 ? Integer.parseInt(args[6]) : 8;
            
            String redundantFile = shuffledFile.replace(".json", "_redundant.json");
            ExperimentUtilities.addRedundantExamples(phase2InputFile, redundantFile, 
                                                     dotFile, numAdditional, minLength, maxLength);
            phase2InputFile = redundantFile;
            
            System.out.println("\nVariations applied. Using modified file for Phase 2: " + phase2InputFile);
        }
        
        // ========================================
        // PHASE 2: Learn with PassiveLearningOracle
        // ========================================
        System.out.println("\n\nPHASE 2: Learning with PassiveLearningOracle");
        System.out.println("================================================\n");
        
        // Deactivate CharacteristicSetGeneratingOracle
        CharacteristicSetGeneratingOracle.active = false;
        
        // Load queries as examples
        System.out.println("Loading queries as examples from: " + phase2InputFile);
        String[] alphabet = PassiveLearningOracle.loadFromJSON(phase2InputFile);
        System.out.println("Alphabet: " + Arrays.toString(alphabet));
        System.out.println("Positive examples: " + PassiveLearningOracle.positiveWords.size());
        System.out.println("Negative examples: " + PassiveLearningOracle.negativeWords.size());
        System.out.println();
        
        // Activate PassiveLearningOracle
        PassiveLearningOracle.active = true;
        PassiveLearningOracle.closedWorld = false; // Fail on unknown words (strict passive learning)
        
        // Reset M2MA state
        M2MA.alphabet = alphabet;
        M2MA.letterToIndex = new HashMap<>();
        for (int i = 0; i < alphabet.length; i++) {
            M2MA.letterToIndex.put(alphabet[i], i);
        }
        M2MA.Hankel = new HashMap<>();
        M2MA.startTime = System.nanoTime();
        
        // Run learning again
        System.out.println("Starting learning process...");
        System.out.println("----------------------------\n");
        M2MA.learn();
        
        // Save Phase 2 results
        HashMap<Integer, ArrayList<Integer>> phase2FinalVector = M2MA.resultFinalVector;
        HashMap<Integer, ArrayList<Integer>>[] phase2TransitionMatrices = M2MA.resultTransitionMatrices;
        int phase2Dimension = M2MA.learnedSize;
        
        System.out.println("\nPhase 2 Results:");
        System.out.println("  Learned dimension: " + phase2Dimension);
        M2MA.displayRuntime();
        
        // ========================================
        // COMPARISON
        // ========================================
        System.out.println("\n\nCOMPARISON");
        System.out.println("==========\n");
        
        System.out.println("Phase 1 (CharacteristicSetGeneratingOracle):");
        System.out.println("  Dimension: " + phase1Dimension);
        System.out.println("  Queries collected: " + CharacteristicSetGeneratingOracle.allQueries.size());
        System.out.println("  Output file: " + queriesFile);
        
        System.out.println("\nPhase 2 (PassiveLearningOracle):");
        System.out.println("  Dimension: " + phase2Dimension);
        System.out.println("  Examples used: " + (PassiveLearningOracle.positiveWords.size() + PassiveLearningOracle.negativeWords.size()));
        if (applyVariations) {
            System.out.println("  (Variations applied: shuffled + redundant examples)");
        }
        
        System.out.println("\nComparison:");
        if (phase1Dimension == phase2Dimension) {
            System.out.println("  ✓ Dimensions match: " + phase1Dimension);
        } else {
            System.out.println("  ✗ Dimensions differ: " + phase1Dimension + " vs " + phase2Dimension);
        }
        
        // Verify Phase 2 against Phase 1
        System.out.println("\nVerifying Phase 2 M2MA against Phase 1 queries...");
        verifyAgainstQueries(phase2FinalVector, phase2TransitionMatrices);
        
        // Verify against original DFA
        System.out.println("\nVerifying Phase 2 M2MA against original DFA...");
        verifyAgainstDFA(phase2FinalVector, phase2TransitionMatrices, dotFile);
        
        // Compare logs
        System.out.println("\n\nLOG COMPARISON");
        System.out.println("==============");
        compareLogs();
        
        System.out.println("\nExperiment completed!");
    }
    
    /**
     * Compare MQ and EQ logs between Phase 1 and Phase 2.
     */
    public static void compareLogs() {
        System.out.println("\nMQ Call Comparison:");
        System.out.println("-------------------");
        System.out.println("Phase 1 (CharacteristicSetGeneratingOracle): " + CharacteristicSetGeneratingOracle.mqLog.size() + " MQ calls");
        System.out.println("Phase 2 (PassiveLearningOracle): " + PassiveLearningOracle.mqLog.size() + " MQ calls");
        
        // Show first 20 MQ calls from each phase
        System.out.println("\nFirst 20 MQ calls from Phase 1:");
        for (int i = 0; i < Math.min(20, CharacteristicSetGeneratingOracle.mqLog.size()); i++) {
            System.out.println("  " + CharacteristicSetGeneratingOracle.mqLog.get(i));
        }
        
        System.out.println("\nFirst 20 MQ calls from Phase 2:");
        for (int i = 0; i < Math.min(20, PassiveLearningOracle.mqLog.size()); i++) {
            System.out.println("  " + PassiveLearningOracle.mqLog.get(i));
        }
        
        System.out.println("\n\nEQ Counterexample Comparison:");
        System.out.println("------------------------------");
        System.out.println("Phase 1 (CharacteristicSetGeneratingOracle): " + CharacteristicSetGeneratingOracle.eqCounterExampleLog.size() + " EQ calls");
        System.out.println("Phase 2 (PassiveLearningOracle): " + PassiveLearningOracle.eqCounterExampleLog.size() + " EQ calls");
        
        // Show all counterexamples
        System.out.println("\nAll counterexamples from Phase 1:");
        for (String entry : CharacteristicSetGeneratingOracle.eqCounterExampleLog) {
            System.out.println("  " + entry);
        }
        
        System.out.println("\nAll counterexamples from Phase 2:");
        for (String entry : PassiveLearningOracle.eqCounterExampleLog) {
            System.out.println("  " + entry);
        }
        
        // Compare counterexamples
        System.out.println("\n\nCounterexample Sequence Comparison:");
        System.out.println("-----------------------------------");
        int minSize = Math.min(CharacteristicSetGeneratingOracle.eqCounterExampleLog.size(), 
                              PassiveLearningOracle.eqCounterExampleLog.size());
        
        boolean allMatch = true;
        for (int i = 0; i < minSize; i++) {
            String phase1 = CharacteristicSetGeneratingOracle.eqCounterExampleLog.get(i);
            String phase2 = PassiveLearningOracle.eqCounterExampleLog.get(i);
            
            // Extract counterexample word (everything after ": ")
            String ce1 = phase1.contains(": ") ? phase1.substring(phase1.indexOf(": ") + 2) : phase1;
            String ce2 = phase2.contains(": ") ? phase2.substring(phase2.indexOf(": ") + 2) : phase2;
            
            if (ce1.equals(ce2)) {
                System.out.println("  EQ #" + (i+1) + ": ✓ Match - " + ce1);
            } else {
                System.out.println("  EQ #" + (i+1) + ": ✗ DIFFERENT");
                System.out.println("    Phase 1: " + ce1);
                System.out.println("    Phase 2: " + ce2);
                allMatch = false;
            }
        }
        
        if (CharacteristicSetGeneratingOracle.eqCounterExampleLog.size() != PassiveLearningOracle.eqCounterExampleLog.size()) {
            System.out.println("\n  ⚠️ Different number of EQ calls!");
            System.out.println("    Phase 1: " + CharacteristicSetGeneratingOracle.eqCounterExampleLog.size());
            System.out.println("    Phase 2: " + PassiveLearningOracle.eqCounterExampleLog.size());
            allMatch = false;
        }
        
        if (allMatch && minSize > 0) {
            System.out.println("\n✓ All counterexamples match!");
        } else if (minSize == 0) {
            System.out.println("\n⚠️ No counterexamples found in either phase");
        }
    }
    
    /**
     * Verify Phase 2 M2MA against all queries from Phase 1.
     */
    public static void verifyAgainstQueries(HashMap<Integer, ArrayList<Integer>> finalVector,
                                           HashMap<Integer, ArrayList<Integer>>[] transitionMatrices) throws Exception {
        int correct = 0;
        int wrong = 0;
        
        for (Map.Entry<String, Integer> entry : CharacteristicSetGeneratingOracle.allQueries.entrySet()) {
            String word = entry.getKey();
            int expected = entry.getValue();
            int actual = M2MA.MQArbitrary(finalVector, transitionMatrices, word);
            
            if (expected == actual) {
                correct++;
            } else {
                wrong++;
                System.out.println("  MISMATCH: word=\"" + word + "\", expected=" + expected + ", got=" + actual);
            }
        }
        
        System.out.println("  Correct: " + correct + "/" + (correct + wrong));
        System.out.println("  Wrong: " + wrong);
        
        if (wrong == 0) {
            System.out.println("  ✓ All queries match!");
        } else {
            System.out.println("  ✗ " + wrong + " mismatches found");
        }
    }
    
    /**
     * Verify Phase 2 M2MA against original DFA.
     */
    public static void verifyAgainstDFA(HashMap<Integer, ArrayList<Integer>> finalVector,
                                        HashMap<Integer, ArrayList<Integer>>[] transitionMatrices,
                                        String dotFile) throws Exception {
        // Reload DFA for verification
        CharacteristicSetGeneratingOracle.loadDFAFromDot(dotFile);
        CharacteristicSetGeneratingOracle.active = true;
        
        // Test on all collected queries
        int correct = 0;
        int wrong = 0;
        
        for (Map.Entry<String, Integer> entry : CharacteristicSetGeneratingOracle.allQueries.entrySet()) {
            String word = entry.getKey();
            int dfaAnswer = CharacteristicSetGeneratingOracle.MQ(word);
            int m2maAnswer = M2MA.MQArbitrary(finalVector, transitionMatrices, word);
            
            if (dfaAnswer == m2maAnswer) {
                correct++;
            } else {
                wrong++;
                System.out.println("  MISMATCH: word=\"" + word + "\", DFA=" + dfaAnswer + ", M2MA=" + m2maAnswer);
            }
        }
        
        // Test on additional random words
        System.out.println("\n  Testing additional random words...");
        int randomCorrect = 0;
        int randomWrong = 0;
        for (int i = 0; i < 100; i++) {
            String test = M2MA.genTest((int) (Math.random() * 10), false);
            int dfaAnswer = CharacteristicSetGeneratingOracle.MQ(test);
            int m2maAnswer = M2MA.MQArbitrary(finalVector, transitionMatrices, test);
            
            if (dfaAnswer == m2maAnswer) {
                randomCorrect++;
            } else {
                randomWrong++;
                if (randomWrong <= 5) { // Show first 5 mismatches
                    System.out.println("  MISMATCH: word=\"" + test + "\", DFA=" + dfaAnswer + ", M2MA=" + m2maAnswer);
                }
            }
        }
        
        System.out.println("\n  Results:");
        System.out.println("    Collected queries: " + correct + "/" + (correct + wrong) + " correct");
        System.out.println("    Random words: " + randomCorrect + "/" + (randomCorrect + randomWrong) + " correct");
        
        int totalCorrect = correct + randomCorrect;
        int totalWrong = wrong + randomWrong;
        
        if (totalWrong == 0) {
            System.out.println("  ✓ Perfect equivalence with original DFA!");
        } else {
            System.out.println("  ✗ " + totalWrong + " mismatches found");
        }
    }
}

