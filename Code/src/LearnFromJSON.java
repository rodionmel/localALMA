/*
 * LearnFromJSON.java
 * 
 * Simple wrapper that loads examples from a JSON file and uses the existing
 * ALMA learning infrastructure (arbitrary.java) with a custom MQ function.
 * 
 * Usage: java LearnFromJSON <input.json> [maxTestLen] [numTests] [eqLimit]
 * 
 * This uses the closed-world assumption: any word not in the positive examples
 * is assumed to be negative.
 */

import java.io.*;
import java.util.*;

public class LearnFromJSON {
    
    public static void main(String[] args) throws Exception {
        System.out.println("LearnFromJSON: Learn M2MA from JSON Examples");
        System.out.println("=============================================\n");
        
        if (args.length < 1) {
            System.out.println("Usage: java LearnFromJSON <input.json> [maxTestLen] [numTests] [eqLimit]");
            System.out.println("  maxTestLen : max length for statistical EQ tests (default: 25)");
            System.out.println("  numTests   : number of statistical EQ tests (default: 1000)");
            System.out.println("  eqLimit    : max number of EQ rounds (default: 50)");
            System.exit(1);
        }
        
        String jsonFile = args[0];
        int maxTestLen = args.length > 1 ? Integer.parseInt(args[1]) : 25;
        int numTests = args.length > 2 ? Integer.parseInt(args[2]) : 1000;
        int eqLimit = args.length > 3 ? Integer.parseInt(args[3]) : 50;
        
        // Load examples from JSON
        System.out.println("Loading examples from: " + jsonFile);
        String[] alphabet = ExampleLoader.loadFromJSON(jsonFile);
        System.out.println("Alphabet: " + Arrays.toString(alphabet));
        System.out.println();
        
        // Create temporary input file for arbitrary.java
        File tempFile = File.createTempFile("alma_input_", ".txt");
        tempFile.deleteOnExit();
        
        PrintWriter writer = new PrintWriter(tempFile);
        writer.println("// Auto-generated input for arbitrary.java");
        writer.println("MQExamples");
        writer.println(maxTestLen);
        writer.println(numTests);
        writer.println(eqLimit);
        // Write alphabet
        for (int i = 0; i < alphabet.length; i++) {
            if (i > 0) writer.print(" ");
            writer.print(alphabet[i]);
        }
        writer.println();
        writer.close();
        
        System.out.println("Created temporary input file: " + tempFile.getAbsolutePath());
        System.out.println("EQ settings: maxLen=" + maxTestLen + ", numTests=" + numTests + ", limit=" + eqLimit);
        System.out.println();
        System.out.println("Starting learning process...");
        System.out.println("----------------------------\n");
        
        // Redirect stdin to read from temp file, then run arbitrary.java logic
        // Instead of running as subprocess, we call the methods directly
        
        // Setup for arbitrary.java
        arbitrary.MQMethod = ExampleLoader.class.getMethod("MQExamples", String.class);
        arbitrary.EQMaxTestLen = maxTestLen;
        arbitrary.EQNumTests = numTests;
        arbitrary.EQLimit = eqLimit;
        arbitrary.EQNumPerformed = 0;
        
        // Setup alphabet in M2MA
        M2MA.alphabet = alphabet;
        M2MA.letterToIndex = new HashMap<>();
        for (int i = 0; i < alphabet.length; i++) {
            M2MA.letterToIndex.put(alphabet[i], i);
        }
        M2MA.Hankel = new HashMap<>();
        M2MA.startTime = System.nanoTime();
        
        // Run the learning algorithm
        M2MA.learn();
        
        // Display learned M2MA before minimization
        System.out.println("Learned M2MA (before minimization)");
        System.out.println("-----------------------------------");
        System.out.println("Dimension: " + M2MA.learnedSize + "\n");
        
        // Minimize the learned result
        System.out.println("Minimizing learned M2MA...");
        minimizeResult();
        
        // Display results
        M2MA.displayResults();
        M2MA.displayRuntime();
        
        // Verify against known examples
        System.out.println("Verifying against known examples...");
        verifyResults();
        
        // Interactive testing (optional)
        System.out.println("\nYou can now test words interactively.");
        M2MA.in = new Scanner(System.in);
        M2MA.operationsOnLearnedMA();
    }
    
    /**
     * Minimize the learned M2MA using the existing minimization algorithm.
     */
    @SuppressWarnings("unchecked")
    public static void minimizeResult() throws Exception {
        // Copy the learned result to input fields for minimization
        M2MA.inputSize = M2MA.learnedSize;
        M2MA.inputFinalVector = M2MA.resultFinalVector;
        M2MA.inputTransitionMatrices = M2MA.resultTransitionMatrices;
        
        // Run minimization
        M2MA.minimize();
        
        int minDim = M2MA.minSize;
        System.out.println("Minimized dimension: " + minDim);
        
        if (minDim < M2MA.learnedSize) {
            System.out.println("(Reduced from " + M2MA.learnedSize + " to " + minDim + ")\n");
            
            // Update the result to use minimized version
            M2MA.resultFinalVector = M2MA.minFinalVector;
            M2MA.resultTransitionMatrices = M2MA.minTransitionMatrices;
            M2MA.learnedSize = minDim;
        } else {
            System.out.println("(Already minimal)\n");
        }
    }
    
    /**
     * Verify the learned M2MA against all known examples.
     */
    public static void verifyResults() throws Exception {
        int correctPos = 0, wrongPos = 0;
        int correctNeg = 0, wrongNeg = 0;
        
        for (String word : ExampleLoader.positiveWords) {
            int result = M2MA.MQArbitrary(M2MA.resultFinalVector, M2MA.resultTransitionMatrices, word);
            if (result == 1) {
                correctPos++;
            } else {
                wrongPos++;
                System.out.println("  WRONG: '" + word + "' should be positive but got negative");
            }
        }
        
        for (String word : ExampleLoader.negativeWords) {
            int result = M2MA.MQArbitrary(M2MA.resultFinalVector, M2MA.resultTransitionMatrices, word);
            if (result == 0) {
                correctNeg++;
            } else {
                wrongNeg++;
                System.out.println("  WRONG: '" + word + "' should be negative but got positive");
            }
        }
        
        System.out.println("Positive examples: " + correctPos + "/" + ExampleLoader.positiveWords.size() + " correct");
        System.out.println("Negative examples: " + correctNeg + "/" + ExampleLoader.negativeWords.size() + " correct");
        
        if (wrongPos == 0 && wrongNeg == 0) {
            System.out.println("\n*** SUCCESS: All examples correctly classified! ***");
        } else {
            System.out.println("\n*** WARNING: " + (wrongPos + wrongNeg) + " examples misclassified ***");
        }
    }
}

