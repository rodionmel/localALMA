/*
 * Author: Nevin George
 * Advisor: Dana Angluin
 * Program Description: The program contains the membership query functions that are used in arbitrary.java. Each
 * function takes in as input a word in the specified language and returns as output either 0 or 1.
 */

public class MQ {
	
	// MQ #1
	public static int MQ1(String w) {
		String[] wArr = w.split(" ");
		if (w.length() == 0) {
			wArr = new String[0];
		}
		
		if (wArr.length%2 == 0) {
			return 1;
		}
		return 0;
	}
	
	// MQ #2
	public static int MQ2(String w) {
		String[] wArr = w.split(" ");
		if (w.length() == 0) {
			wArr = new String[0];
		}
		
		if (wArr.length%10 == 0) {
			return 1;
		}
		return 0;
	}
	
	// MQ #3
	public static int MQ3(String w) {
		String[] wArr = w.split(" ");
		if (w.length() == 0) {
			wArr = new String[0];
		}
		
		boolean res_0 = (p_i(wArr, 0) || q_i(wArr, 0));
		return (res_0) ? 1 : 0;
	}

	// MQ #4
	public static int MQ4(String w) {
		String[] wArr = w.split(" ");
		if (w.length() == 0) {
			wArr = new String[0];
		}
		
		boolean res_0 = (p_i(wArr, 0) || q_i(wArr, 0));
		boolean res_1 = (p_i(wArr, 1) || q_i(wArr, 1));
		return (res_0 && res_1) ? 1 : 0;
	}

	// MQ #5
	public static int MQ5(String w) {
		String[] wArr = w.split(" ");
		if (w.length() == 0) {
			wArr = new String[0];
		}
		
		boolean res_0 = (p_i(wArr, 0) || q_i(wArr, 0));
		boolean res_1 = (p_i(wArr, 1) || q_i(wArr, 1));
		boolean res_2 = (p_i(wArr, 2) || q_i(wArr, 2));
		return (res_0 && res_1 && res_2) ? 1 : 0;
	}


	// MQ #6
	public static int MQ6(String w) {
		String[] wArr = w.split(" ");
		if (w.length() == 0) {
			wArr = new String[0];
		}
		
		if (! checkNumOfDollars(wArr)) {
			return 0;
		}
		
		boolean res_0 = (p_i(wArr, 0) || q_i(wArr, 0));
		boolean res_1 = (p_i(wArr, 1) || q_i(wArr, 1));
		boolean res_2 = (p_i(wArr, 2) || q_i(wArr, 2));
		boolean res_3 = (p_i(wArr, 3) || q_i(wArr, 3));
		return (res_0 && res_1 && res_2 && res_3) ? 1 : 0;
	}

	// MQ #7
	public static int MQ7(String w) {
		String[] wArr = w.split(" ");
		if (w.length() == 0) {
			wArr = new String[0];
		}
		
		if (! checkNumOfDollars(wArr)) {
			return 0;
		}
		
		boolean res_0 = (p_i(wArr, 0) || q_i(wArr, 0));
		boolean res_1 = (p_i(wArr, 1) || q_i(wArr, 1));
		boolean res_2 = (p_i(wArr, 2) || q_i(wArr, 2));
		boolean res_3 = (p_i(wArr, 3) || q_i(wArr, 3));
		boolean res_4 = (p_i(wArr, 4) || q_i(wArr, 4));
		return (res_0 && res_1 && res_2 && res_3 && res_4) ? 1 : 0;
	}

	static boolean checkNumOfDollars(String[] wArr) {
		boolean dollarFound = false;
		for (String l: wArr) {
			if (l.equals("$")) {
				if (dollarFound) {
					return false;
				}
				else {
					dollarFound = true;
				}
			}
		}
		return dollarFound;
	}

	static boolean q_i(String[] wArr, int i) {
		int state = 0; // initial, prefix = 0, $ found = 1, postfix OK = 2, error = 3 
		for (String l: wArr) {
			boolean isDollar = l.equals("$");
			boolean isQ = !isDollar && l.charAt(i*2+1) == 'q';
			
			if (state == 0) {
				if (isDollar) {
					state = 1;
				}
			}
			else if (state == 1) {
				if (isQ) {
					state = 2;
				}
				else {
					state = 3;
				}
			}
			else if (state == 2) {
				if (! isQ) {
					state = 3;
				}
			}
			else if (state == 3) {
				return false;
			}
		}
		
		if (state == 2) {
			return true;
		}
		else {
			return false;
		}
	}
	
	static boolean p_i(String[] wArr, int i) {
		int state = 0; // initial, prefix = 0, $ found = 1, postfix = 2, postfix OK = 3, error = 4
		
		for (String l: wArr) {
			boolean isDollar = l.equals("$");
			boolean isP = ! isDollar && l.charAt(i*2) == 'p';
			
			if (state == 0) {
				if (isDollar) {
					state = 1;
				}
			}
			else if (state == 1) {
				if (isDollar) {
					state = 4;
				}
				else if (isP) {
					state = 3;
				}
				else {
					state = 2;
				}
			}
			else if (state == 2) {
				if (isDollar) {
					state = 4;
				}
				else if (isP) {
					state = 3;
				}
			}
			else if (state == 3) {
				if (isDollar) {
					state = 4;
				}
			}
			else if (state == 4) {
				return false;
			}
		}
		
		if (state == 3) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public static int MQ_F2_i1(String w) {
		String[] wArr = w.split(" ");
		if (w.length() == 0) {
			wArr = new String[0];
		}
		
		if (! checOneDollarPostfixNonEmpty(wArr)) {
			return 0;
		}

		boolean res = F2_p_i(wArr, 0) || F2_q_i(wArr, 0);
		return res ? 1 : 0;
	}

	public static int MQ_F2_i2(String w) {
		String[] wArr = w.split(" ");
		if (w.length() == 0) {
			wArr = new String[0];
		}
		
		if (! checOneDollarPostfixNonEmpty(wArr)) {
			return 0;
		}

		boolean res1 = F2_p_i(wArr, 0) || F2_q_i(wArr, 0);
		boolean res2 = F2_p_i(wArr, 1) || F2_q_i(wArr, 1);
		return res1 && res2 ? 1 : 0;
	}

	public static int MQ_F2_i3(String w) {
		String[] wArr = w.split(" ");
		if (w.length() == 0) {
			wArr = new String[0];
		}
		
		if (! checOneDollarPostfixNonEmpty(wArr)) {
			return 0;
		}

		boolean res1 = F2_p_i(wArr, 0) || F2_q_i(wArr, 0);
		boolean res2 = F2_p_i(wArr, 1) || F2_q_i(wArr, 1);
		boolean res3 = F2_p_i(wArr, 2) || F2_q_i(wArr, 2);
		return res1 && res2 && res3 ? 1 : 0;
	}

	public static int MQ_F2_i4(String w) {
		String[] wArr = w.split(" ");
		if (w.length() == 0) {
			wArr = new String[0];
		}
		
		if (! checOneDollarPostfixNonEmpty(wArr)) {
			return 0;
		}

		boolean res1 = F2_p_i(wArr, 0) || F2_q_i(wArr, 0);
		boolean res2 = F2_p_i(wArr, 1) || F2_q_i(wArr, 1);
		boolean res3 = F2_p_i(wArr, 2) || F2_q_i(wArr, 2);
		boolean res4 = F2_p_i(wArr, 3) || F2_q_i(wArr, 3);
		return res1 && res2 && res3 && res4 ? 1 : 0;
	}

	public static int MQ_F2_i5(String w) {
		String[] wArr = w.split(" ");
		if (w.length() == 0) {
			wArr = new String[0];
		}
		
		if (! checOneDollarPostfixNonEmpty(wArr)) {
			return 0;
		}

		boolean res1 = F2_p_i(wArr, 0) || F2_q_i(wArr, 0);
		boolean res2 = F2_p_i(wArr, 1) || F2_q_i(wArr, 1);
		boolean res3 = F2_p_i(wArr, 2) || F2_q_i(wArr, 2);
		boolean res4 = F2_p_i(wArr, 3) || F2_q_i(wArr, 3);
		boolean res5 = F2_p_i(wArr, 4) || F2_q_i(wArr, 4);
		return res1 && res2 && res3 && res4 && res5 ? 1 : 0;
	}

	static boolean checOneDollarPostfixNonEmpty(String[] wArr) {
		int state = 0; // 0 - dollar not found, 1 - dollar found, 2 - postfix non-empty, 3 - error
		for (String l: wArr) {
			if (l.equals("$")) {
				if (state > 0) {
					state = 3;
					break;
				}
				else {
					state = 1;
				}
			}
			else if (state == 1) {
				state = 2;
			}
		}
		return state == 2;
	}
	
	static boolean F2_p_i(String[] wArr, int i) {
		int state = 0; // 0 - not found, 1 - found OK, 2 - error
		
		for (String l: wArr) {
			boolean isDollar = l.equals("$");
			if (isDollar) {
				continue;
			}
			boolean isP = ! isDollar && l.charAt(i*2) == 'p';
			
			switch (state) {
			case 0:
				if (isP) {
					state = 1;
				}
				else {
					return false;
				}
				break;
			case 1:
				if (!isP) {
					return false;
				}
				break;
			}
		}
		return state == 1;
	}

	static boolean F2_q_i(String[] wArr, int i) {
		int state = 0; // 0 - q not found, 1 - q found
		
		for (String l: wArr) {
			boolean isDollar = l.equals("$");
			if (isDollar) {
				continue;
			}
			boolean isQ = !isDollar && l.charAt(i*2+1) == 'q';
			
			if (isQ) {
				return true;
			}
		}
		return false;
	}
//	
//	public static int MQF10(String w) {
//		String[] wArr = w.split(" ");
//		if (w.length() == 0) {
//			wArr = new String[0];
//		}
//		
//		if (! checkNumOfDollars(wArr)) {
//			return 0;
//		}
//		
//		int state = 0; // 0 - $ not found, 1 - $ found,  4 error
//		
//		for (String l: wArr) {
//			boolean isDollar = l.equals("$");
//			boolean isP = !isDollar && l.charAt(0) == 'p';
//			boolean isQ = !isDollar && l.charAt(1) == 'q';
//
//			switch (state) {
//			case 0:
//				if (isP && ! isQ) {
//					state = 4;
//				}
//				else if (isDollar) {
//					state = 1;
//				}
//				break;
//			case 1:
//				if (isP && !isQ) {
//					state = 4;
//				}
//				break;
//			case 4:
//				return 0;
//			}
//		}
//		
//		if (state == 1 || state == 3) {
//			return 1;
//		} else {
//			return 0;
//		}
//	}
//	
//	public static int MQF11(String w) {
//		String[] wArr = w.split(" ");
//		if (w.length() == 0) {
//			wArr = new String[0];
//		}
//		
//		if (! checkNumOfDollars(wArr)) {
//			return 0;
//		}
//			
//		return mq_f1_i(wArr, 1);
//	}
//
//	public static int MQF12(String w) {
//		String[] wArr = w.split(" ");
//		if (w.length() == 0) {
//			wArr = new String[0];
//		}
//		
//		if (! checkNumOfDollars(wArr)) {
//			return 0;
//		}
//			
//		return mq_f1_i(wArr, 2);
//	}
//		
//
//	public static int MQF13(String w) {
//		String[] wArr = w.split(" ");
//		if (w.length() == 0) {
//			wArr = new String[0];
//		}
//		
//		if (! checkNumOfDollars(wArr)) {
//			return 0;
//		}
//			
//		return mq_f1_i(wArr, 3);
//	}
//	
//
//	public static int MQF14(String w) {
//		String[] wArr = w.split(" ");
//		if (w.length() == 0) {
//			wArr = new String[0];
//		}
//		
//		if (! checkNumOfDollars(wArr)) {
//			return 0;
//		}
//			
//		return mq_f1_i(wArr, 4);
//	}
//		
//	
//	static int mq_f1_i(String[] wArr, int i) {
//		int state = 0; // 0 - $ not found, 1 - $ found, 4 error
//		int N_counter = 0;
//		
//		for (String l: wArr) {
//			boolean isDollar = l.equals("$");
//			boolean isP = !isDollar && l.charAt(0) == 'p';
//			boolean isQ = !isDollar && l.charAt(1) == 'q';
//
//			switch (state) {
//			case 0:
//				if (isQ) {
//					N_counter = 0;
//				}
//				else {
//					if (isDollar) {
//						state = 1;
//					}
//					else if (N_counter == i) {
//						state = 4;
//					}
//					else if (N_counter != 0) {
//						N_counter += 1;
//					}
//					else if (isP) {
//						N_counter = 1;
//					}
//				}
//				break;
//			case 1:
//				if (isQ) {
//					N_counter = 0;
//				}
//				else {
//					if (N_counter == i) {
//						state = 4;
//					}
//					else if (N_counter != 0) {
//						N_counter += 1;
//					}
//					else if (isP) {
//						N_counter = 1;
//					}
//				}
//				break;
//			case 4:
//				return 0;
//			}
//		}
//		
//		if (state == 1) {
//			return 1;
//		} else {
//			return 0;
//		}
//	}
	
}
