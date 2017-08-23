import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StringsTest {


	static int maxMoves(String s, String t) {
		if (t.length() > s.length() || !s.contains(t)) {
			return 0;
		}
        String sString = new String(s);
		int ret = 0;
		boolean foundOne = false;
		do {
			foundOne = false;
			for (int i = 0; i < s.length(); i++) {
				if (sString.charAt(i) == t.charAt(0)) {
					String check = "";
					if (i+t.length() <= sString.length()) {
						check = sString.substring(i, i+t.length());
					}
					if (check.equals(t)) {
						sString = sString.substring(0, i) + sString.substring(i+t.length(), s.length());
						ret++;
						foundOne = true;
                        break;
					}
				}    
			}
		} while (foundOne == true);
		return ret;
	}

	static int places(int x) {
		int ret = 1;
		while (x/10 != 0) {
			x = x/10;
			ret++;
		}
		return ret;
	}

	static int[] intToArr(int n, int places) {
		int[] ret = new int[places];
		for (int i = places-1; i > -1; i--) {
			ret[i] = n % 10;
			n = n/10;
		}
		return ret;
	}

	static int minimumMoves(int[] a, int[] m) {
		int ret = 0;
		for (int i = 0; i < m.length; i++) {
			int[] aIn = intToArr(a[i], places(a[i]));
			int[] mIn = intToArr(m[i], places(m[i]));
			System.out.println("aIn is " + Arrays.toString(aIn));
			System.out.println("mIn is " + Arrays.toString(mIn));
			for (int j = 0; j < mIn.length; j++) {
				System.out.println("done with while");
				while (aIn[j] != mIn[j]) {
					ret++;
					System.out.println("a is " + aIn[j] + " m is " + mIn[j]);
					System.out.println("j is " + j);
					System.out.println("ret is " + ret);
					if (aIn[j] == mIn[j]) {	
					} else if (aIn[j] < mIn[j]){
						aIn[j]++;
					} else {
						aIn[j]--;
					}
				}
			}
		}
		return ret;
	}

	
	  static int countPalindromes(String s) {
		  	int ret = 0;
		  	ArrayList<String> substrings = allSubstring(s);
		  	for (String substring : substrings) {
		  		if (isPalindrome(s)) {
		  			ret++;
		  		}
		  	}
		  	return ret;
	    }
	  
	  static ArrayList<String> allSubstring(String s) {
		  ArrayList<String> substrings = new ArrayList<>();
		  StringBuilder sta = new StringBuilder(s);
 		  for (int i = 0; i < s.length(); i++) { 
			  int len = i+1;
			  while (len <= s.length()) {
				  StringBuilder st = new StringBuilder(sta.substring(i, len));
				  
				  len++;
				  substrings.add(st.toString());
			  }
		  }
		  return substrings;
	  }
	  
	  static boolean isPalindrome(String s) {
		  int j = s.length()-1;
		  for (int i = 0; i < s.length()/2; i ++) {
			  if (s.charAt(i) != s.charAt(j)) {
				  return false;
			  }
			  j--;
		  }
		  return true;
	  }
	


	public static void main(String[] args) {
				String s = "aabb";
				String t = "ab";
				System.out.println(maxMoves(s, t));

//		int[] a = {1234,4321};
//		int[] m = {2345, 3214};
////		System.out.println(minimumMoves(a, m));
//		
//		allSubstring("abccba");
//		System.out.println(isPalindrome("aaba"));
	}

}
