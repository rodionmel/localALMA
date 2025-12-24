Subject: Research Landscape: Algorithm Capability by Example Type

Hi Dana and Dror,

I wanted to share a quick overview of where we stand with algorithm capability across different example types.

**Note on M2MA Characteristic Sets**: M2MA characteristic sets are tentatively defined by Nevin's active learning algorithm. There's an unproved assumption that some M2MA characteristic sets might not contain enough information to build an exact DFA for the language.

**Algorithm Capability by Example Type**

*(Note: The table shows worst-case scenarios.)*

| Algorithm | Random | DFA Characteristic Set | M2MA Characteristic Set |
|-----------|--------|------------------------|-------------------------|
| RPNI      | Approximate solution | Exact solution | ??? (any guarantee?) |
| Heule     | Exact solution | Exact solution | ??? (any guarantee?) |
| Nevin     | Failure | Failure | Exact solution |
| Rodion    | Exact solution | Exact solution | Exact solution |

**Key observations:**

- **RPNI**: Gets exact solutions on DFA characteristic sets, but only approximate on random examples. Status on M2MA characteristic sets is unclear—if the assumption is correct, might only give approximate solutions.

- **Heule**: Gets exact solutions on both random examples and DFA characteristic sets. Status on M2MA characteristic sets is unclear—if the assumption is correct, might only give approximate solutions.

- **Nevin**: Only works on M2MA characteristic sets (which are defined by Nevin's own active learning algorithm). Fails on random examples and DFA characteristic sets.

- **Rodion**: Gets exact solutions across all three example types.

I'm wondering if it's worth investigating the unknown parts of this landscape now. If the assumption about M2MA characteristic sets is correct, this could make for an interesting story.

Best,
Rodion
