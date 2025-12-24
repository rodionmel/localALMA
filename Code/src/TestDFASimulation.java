/*
 * TestDFASimulation.java
 * 
 * Test to manually verify DFA simulation is working correctly.
 */

import java.io.*;
import java.util.*;

public class TestDFASimulation {
    
    public static void main(String[] args) throws Exception {
        System.out.println("Testing DFA Simulation");
        System.out.println("=======================\n");
        
        // Load DFA
        CharacteristicSetGeneratingOracle.loadDFAFromDot("test_examples/F1_last_2_is_a_ab_dfa.dot");
        
        System.out.println("DFA Structure:");
        System.out.println("  Start state: " + CharacteristicSetGeneratingOracle.startState);
        System.out.println("  Accepting states: " + CharacteristicSetGeneratingOracle.acceptingStates);
        System.out.println("  Transitions:");
        for (Map.Entry<String, Map<String, String>> entry : CharacteristicSetGeneratingOracle.transitions.entrySet()) {
            System.out.println("    State " + entry.getKey() + ":");
            for (Map.Entry<String, String> trans : entry.getValue().entrySet()) {
                System.out.println("      " + trans.getKey() + " -> " + trans.getValue());
            }
        }
        System.out.println();
        
        // Test words manually
        String[] testWords = {"aa", "ab", "baa", "bab", "a", "b", "aba", "abb"};
        
        System.out.println("Manual DFA Simulation:");
        System.out.println("----------------------");
        for (String word : testWords) {
            System.out.println("\nTesting: \"" + word + "\"");
            String currentState = CharacteristicSetGeneratingOracle.startState;
            System.out.println("  Start state: " + currentState);
            
            String[] symbols = word.isEmpty() ? new String[0] : word.split(" ");
            if (word.isEmpty()) {
                symbols = new String[0];
            } else {
                // Convert to space-separated format
                symbols = word.replace("", " ").trim().split("\\s+");
                if (symbols.length == 1 && symbols[0].isEmpty()) {
                    symbols = new String[0];
                }
            }
            
            System.out.println("  Symbols: " + Arrays.toString(symbols));
            
            for (int i = 0; i < symbols.length; i++) {
                String symbol = symbols[i];
                Map<String, String> stateTransitions = CharacteristicSetGeneratingOracle.transitions.get(currentState);
                if (stateTransitions == null || !stateTransitions.containsKey(symbol)) {
                    System.out.println("  -> No transition from " + currentState + " on " + symbol + " -> REJECT");
                    break;
                }
                currentState = stateTransitions.get(symbol);
                System.out.println("  -> " + symbol + " -> State " + currentState);
            }
            
            boolean accepted = CharacteristicSetGeneratingOracle.acceptingStates.contains(currentState);
            System.out.println("  Final state: " + currentState + " (accepting: " + accepted + ")");
            System.out.println("  Result: " + (accepted ? "ACCEPT" : "REJECT"));
        }
        
        System.out.println("\n\nUsing Oracle MQ:");
        System.out.println("-----------------");
        CharacteristicSetGeneratingOracle.active = true;
        for (String word : testWords) {
            int result = CharacteristicSetGeneratingOracle.MQ(word);
            System.out.println("  MQ(\"" + word + "\") = " + result);
        }
    }
}

