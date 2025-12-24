/*
 * LearnFromPassiveOracle.java
 * 
 * Wrapper program for using PassiveLearningOracle with M2MA learning.
 * Loads examples from JSON and runs the learning algorithm.
 * 
 * Usage: java LearnFromPassiveOracle <input.json> [closedWorld]
 *   closedWorld: "true" or "false" (default: false)
 */

import java.io.*;
import java.util.*;

public class LearnFromPassiveOracle {
    
    public static void main(String[] args) throws Exception {
        System.out.println("LearnFromPassiveOracle: Learn M2MA from Passive Learning Oracle");
        System.out.println("================================================================\n");
        
        if (args.length < 1) {
            System.out.println("Usage: java LearnFromPassiveOracle <input.json> [closedWorld]");
            System.out.println("  input.json  : JSON file with positive and negative examples");
            System.out.println("  closedWorld : true/false - treat unknown words as negative (default: false)");
            System.exit(1);
        }
        
        String jsonFile = args[0];
        boolean closedWorld = args.length > 1 ? Boolean.parseBoolean(args[1]) : false;
        
        // Load examples from JSON
        System.out.println("Loading examples from: " + jsonFile);
        String[] alphabet = PassiveLearningOracle.loadFromJSON(jsonFile);
        System.out.println("Alphabet: " + Arrays.toString(alphabet));
        System.out.println("Closed-world assumption: " + closedWorld);
        System.out.println();
        
        // Configure oracle
        PassiveLearningOracle.active = true;
        PassiveLearningOracle.closedWorld = closedWorld;
        
        // Set up alphabet in M2MA
        M2MA.alphabet = alphabet;
        M2MA.letterToIndex = new HashMap<>();
        for (int i = 0; i < alphabet.length; i++) {
            M2MA.letterToIndex.put(alphabet[i], i);
        }
        M2MA.Hankel = new HashMap<>();
        M2MA.startTime = System.nanoTime();
        
        // Run the learning algorithm
        System.out.println("Starting learning process...");
        System.out.println("----------------------------\n");
        M2MA.learn();
        
        // Display results
        M2MA.displayResults();
        M2MA.displayRuntime();
        
        // Verify against known examples
        System.out.println("\nVerifying against known examples...");
        verifyResults();
        
        // Interactive testing (optional)
        System.out.println("\nYou can now test words interactively.");
        M2MA.in = new Scanner(System.in);
        M2MA.operationsOnLearnedMA();
    }
    
    /**
     * Verify the learned M2MA against all known examples.
     */
    public static void verifyResults() throws Exception {
        int correctPos = 0, wrongPos = 0;
        int correctNeg = 0, wrongNeg = 0;
        
        for (String word : PassiveLearningOracle.positiveWords) {
            int result = M2MA.MQArbitrary(M2MA.resultFinalVector, M2MA.resultTransitionMatrices, word);
            if (result == 1) {
                correctPos++;
            } else {
                wrongPos++;
                System.out.println("  WRONG: '" + word + "' should be positive but got negative");
            }
        }
        
        for (String word : PassiveLearningOracle.negativeWords) {
            int result = M2MA.MQArbitrary(M2MA.resultFinalVector, M2MA.resultTransitionMatrices, word);
            if (result == 0) {
                correctNeg++;
            } else {
                wrongNeg++;
                System.out.println("  WRONG: '" + word + "' should be negative but got positive");
            }
        }
        
        System.out.println("Positive examples: " + correctPos + "/" + PassiveLearningOracle.positiveWords.size() + " correct");
        System.out.println("Negative examples: " + correctNeg + "/" + PassiveLearningOracle.negativeWords.size() + " correct");
        
        if (wrongPos == 0 && wrongNeg == 0) {
            System.out.println("\n*** SUCCESS: All examples correctly classified! ***");
        } else {
            System.out.println("\n*** WARNING: " + (wrongPos + wrongNeg) + " examples misclassified ***");
        }
    }
}

