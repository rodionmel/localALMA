/*
 * LearnFromDFA.java
 * 
 * Wrapper program for using CharacteristicSetGeneratingOracle with M2MA learning.
 * Loads a DFA from a .dot file and runs the learning algorithm.
 * 
 * Usage: java LearnFromDFA <input.dot> [maxTestLen] [numTests]
 *   maxTestLen: maximum length for EQ test strings (default: 25)
 *   numTests  : number of EQ test strings (default: 1000)
 */

import java.io.*;
import java.util.*;

public class LearnFromDFA {
    
    public static void main(String[] args) throws Exception {
        System.out.println("LearnFromDFA: Learn M2MA from DFA using CharacteristicSetGeneratingOracle");
        System.out.println("==========================================================================\n");
        
        if (args.length < 1) {
            System.out.println("Usage: java LearnFromDFA <input.dot> [maxTestLen] [numTests]");
            System.out.println("  input.dot  : Graphviz DOT file with DFA definition");
            System.out.println("  maxTestLen : max length for EQ test strings (default: 25)");
            System.out.println("  numTests   : number of EQ test strings (default: 1000)");
            System.exit(1);
        }
        
        String dotFile = args[0];
        int maxTestLen = args.length > 1 ? Integer.parseInt(args[1]) : 25;
        int numTests = args.length > 2 ? Integer.parseInt(args[2]) : 1000;
        
        // Load DFA from DOT file
        System.out.println("Loading DFA from: " + dotFile);
        CharacteristicSetGeneratingOracle.loadDFAFromDot(dotFile);
        System.out.println();
        
        // Configure EQ parameters
        CharacteristicSetGeneratingOracle.setEQParameters(maxTestLen, numTests);
        System.out.println("EQ settings: maxTestLen=" + maxTestLen + ", numTests=" + numTests);
        System.out.println();
        
        // Activate oracle
        CharacteristicSetGeneratingOracle.active = true;
        
        // Set up alphabet in M2MA
        M2MA.alphabet = CharacteristicSetGeneratingOracle.alphabet;
        M2MA.letterToIndex = new HashMap<>();
        for (int i = 0; i < M2MA.alphabet.length; i++) {
            M2MA.letterToIndex.put(M2MA.alphabet[i], i);
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
        
        // Print query statistics
        System.out.println();
        CharacteristicSetGeneratingOracle.printQueryStatistics();
        
        // Output collected queries to JSON file
        String outputFile = dotFile.replace(".dot", "_queries.json");
        if (outputFile.equals(dotFile)) {
            outputFile = dotFile + "_queries.json";
        }
        System.out.println();
        CharacteristicSetGeneratingOracle.outputQueriesToJSON(outputFile);
        
        // Interactive testing (optional)
        System.out.println("\nYou can now test words interactively.");
        M2MA.in = new Scanner(System.in);
        M2MA.operationsOnLearnedMA();
    }
}

