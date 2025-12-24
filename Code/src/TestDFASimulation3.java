/*
 * TestDFASimulation3.java
 * 
 * Test with fresh DFA load to avoid caching issues.
 */

import java.io.*;
import java.util.*;

public class TestDFASimulation3 {
    
    public static void main(String[] args) throws Exception {
        System.out.println("Fresh DFA Load Test");
        System.out.println("===================\n");
        
        // Load DFA fresh
        CharacteristicSetGeneratingOracle.loadDFAFromDot("test_examples/F1_last_2_is_a_ab_dfa.dot");
        CharacteristicSetGeneratingOracle.active = true;
        
        System.out.println("allQueries size before: " + CharacteristicSetGeneratingOracle.allQueries.size());
        System.out.println("Testing 'aa' for first time...\n");
        
        // Test "aa"
        String w = "aa";
        String currentState = CharacteristicSetGeneratingOracle.startState;
        System.out.println("Word: \"" + w + "\"");
        System.out.println("Start state: " + currentState);
        
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
        System.out.println("Symbols: " + Arrays.toString(symbols));
        
        for (String symbol : symbols) {
            Map<String, String> stateTransitions = CharacteristicSetGeneratingOracle.transitions.get(currentState);
            System.out.println("State " + currentState + " on '" + symbol + "': " + stateTransitions);
            if (stateTransitions == null || !stateTransitions.containsKey(symbol)) {
                System.out.println("NO TRANSITION - REJECT");
                return;
            }
            currentState = stateTransitions.get(symbol);
            System.out.println("-> State " + currentState);
        }
        
        boolean accepted = CharacteristicSetGeneratingOracle.acceptingStates.contains(currentState);
        System.out.println("\nFinal state: " + currentState);
        System.out.println("Accepting states: " + CharacteristicSetGeneratingOracle.acceptingStates);
        System.out.println("Is accepting: " + accepted);
        System.out.println("Should return: " + (accepted ? 1 : 0));
        
        // Now call actual MQ
        System.out.println("\n--- Calling MQ ---");
        int result = CharacteristicSetGeneratingOracle.MQ(w);
        System.out.println("MQ returned: " + result);
        System.out.println("allQueries size after: " + CharacteristicSetGeneratingOracle.allQueries.size());
        System.out.println("allQueries.get(\"aa\"): " + CharacteristicSetGeneratingOracle.allQueries.get("aa"));
    }
}



