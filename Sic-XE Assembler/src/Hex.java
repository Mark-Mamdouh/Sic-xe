public class Hex {

	public String toHex(String s) {
		return Integer.toHexString((Integer.parseInt(s)));
	}

	public String toHex2(String s) {
		String ss = Integer.toHexString((Integer.parseInt(s)));
		if (ss.length() % 2 != 0) {
			ss = "0" + ss;
		}
		return ss;
	}

	public String charToHex(String s) {
		String ss = "";
		for (int i = 0; i < s.length(); i++) {
			ss += String.format("%02x", (int) s.charAt(i));
		}
		return ss;

	}

	public String toDec(String s) {
		return String.valueOf(Integer.parseInt(s, 16));
	}

	public String addHex(String s1, String s2) {
		String ss1 = toDec(s1);
		String ss2 = toDec(s2);
		int sss = Integer.parseInt(ss1) + Integer.parseInt(ss2);
		String ssss = String.valueOf(sss);
		String ans = toHex(ssss);
		return ans;
	}

	public String addDec(String s1, String s2) {
		return String.valueOf(Integer.parseInt(toDec(s1) + Integer.parseInt(toDec(s2))));
	}

	public String subHex(String s1, String s2) {
		String s1dc = toDec(s1);
		String s2dc = toDec(s2);
		int diff = Integer.parseInt(s1dc) - Integer.parseInt(s2dc);
		String diffst = String.valueOf(diff);
		return toHex(diffst);
	}

	public String HexOr(String s1, String s2) {
		String s1dec = toDec(s1);
		String s2dec = toDec(s2);
		int s1int = Integer.valueOf(s1dec);
		int s2int = Integer.valueOf(s2dec);
		int res = s1int | s2int;
		return toHex(String.valueOf(res));
	}

	public boolean IsHex(String s) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c >= '0' && c <= '9')
				continue;
			if (c >= 'a' && c <= 'f')
				continue;
			if (c >= 'A' && c <= 'F')
				continue;
			else
				return false;
		}
		return true;
	}

}
