/*
 * TestDFASimulation2.java
 * 
 * Detailed test to debug DFA simulation.
 */

import java.io.*;
import java.util.*;

public class TestDFASimulation2 {
    
    public static void main(String[] args) throws Exception {
        System.out.println("Detailed DFA Simulation Test");
        System.out.println("============================\n");
        
        // Load DFA
        CharacteristicSetGeneratingOracle.loadDFAFromDot("test_examples/F1_last_2_is_a_ab_dfa.dot");
        CharacteristicSetGeneratingOracle.active = true;
        
        // Test "aa" step by step
        String w = "aa";
        System.out.println("Testing word: \"" + w + "\"");
        System.out.println("Contains space: " + w.contains(" "));
        
        String[] symbols;
        if (w.isEmpty()) {
            symbols = new String[0];
        } else if (w.contains(" ")) {
            symbols = w.split(" ");
        } else {
            symbols = new String[w.length()];
            for (int i = 0; i < w.length(); i++) {
                symbols[i] = String.valueOf(w.charAt(i));
            }
        }
        
        System.out.println("Symbols array: " + Arrays.toString(symbols));
        System.out.println("Start state: " + CharacteristicSetGeneratingOracle.startState);
        System.out.println("Accepting states: " + CharacteristicSetGeneratingOracle.acceptingStates);
        
        String currentState = CharacteristicSetGeneratingOracle.startState;
        for (int i = 0; i < symbols.length; i++) {
            String symbol = symbols[i];
            System.out.println("\nStep " + (i+1) + ": symbol = \"" + symbol + "\", current state = " + currentState);
            
            Map<String, Map<String, String>> transitions = CharacteristicSetGeneratingOracle.transitions;
            Map<String, String> stateTransitions = transitions.get(currentState);
            
            System.out.println("  State transitions for " + currentState + ": " + stateTransitions);
            System.out.println("  Has transition for \"" + symbol + "\": " + (stateTransitions != null && stateTransitions.containsKey(symbol)));
            
            if (stateTransitions == null || !stateTransitions.containsKey(symbol)) {
                System.out.println("  -> REJECT (no transition)");
                return;
            }
            currentState = stateTransitions.get(symbol);
            System.out.println("  -> Next state: " + currentState);
        }
        
        boolean accepted = CharacteristicSetGeneratingOracle.acceptingStates.contains(currentState);
        System.out.println("\nFinal state: " + currentState);
        System.out.println("Is accepting: " + accepted);
        System.out.println("Expected result: " + (accepted ? 1 : 0));
        
        // Now test with actual MQ
        System.out.println("\n\nActual MQ result:");
        int result = CharacteristicSetGeneratingOracle.MQ(w);
        System.out.println("MQ(\"" + w + "\") = " + result);
        System.out.println("Stored in allQueries: " + CharacteristicSetGeneratingOracle.allQueries.get(w));
    }
}

