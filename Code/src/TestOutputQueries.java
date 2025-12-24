/*
 * TestOutputQueries.java
 * 
 * Simple test to verify outputQueriesToJSON functionality.
 */

import java.io.*;
import java.util.*;

public class TestOutputQueries {
    
    public static void main(String[] args) throws Exception {
        System.out.println("Testing outputQueriesToJSON");
        System.out.println("===========================\n");
        
        // Load DFA
        CharacteristicSetGeneratingOracle.loadDFAFromDot("test_examples/F1_last_2_is_a_ab_dfa.dot");
        CharacteristicSetGeneratingOracle.active = true;
        
        // Make some queries
        System.out.println("Making test queries...");
        CharacteristicSetGeneratingOracle.MQ("aa");
        CharacteristicSetGeneratingOracle.MQ("ab");
        CharacteristicSetGeneratingOracle.MQ("baa");
        CharacteristicSetGeneratingOracle.MQ("bab");
        CharacteristicSetGeneratingOracle.MQ("a");
        CharacteristicSetGeneratingOracle.MQ("b");
        CharacteristicSetGeneratingOracle.MQ("aba");
        CharacteristicSetGeneratingOracle.MQ("abb");
        
        // Print statistics
        System.out.println();
        CharacteristicSetGeneratingOracle.printQueryStatistics();
        
        // Output to JSON
        System.out.println();
        String outputFile = "test_examples/test_output.json";
        CharacteristicSetGeneratingOracle.outputQueriesToJSON(outputFile);
        
        // Display the output file
        System.out.println("\nOutput file contents:");
        System.out.println("---------------------");
        BufferedReader reader = new BufferedReader(new FileReader(outputFile));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        reader.close();
        
        System.out.println("\nTest completed!");
    }
}

