import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

public class Line {
	public String loc;
	public String label;
	public String operation;
	public String operand;
	public String objcode;
	public String comment;
	public int formatType;
	public Optable opcodes;
	public Hex hxop;
	public boolean allComment;

	public Line() {
		loc = null;
		label = null;
		operation = null;
		operand = null;
		objcode = null;
		opcodes = null;
		comment = null;
		formatType = 0;
		allComment = false;
		hxop = new Hex();
	}

	public Line(String loc, String label, String operation, String operand, String objcode, Optable opcodes) {

		this.loc = loc;
		this.label = label;
		this.operation = operation;
		this.operand = operand;
		this.objcode = objcode;
		this.opcodes = opcodes;
		comment = null;
		allComment = false;
		hxop = new Hex();
	}

	public void setobjcode(String objcode) {
		this.objcode = objcode;
	}

	public void calObjcode(String operandLoc) {
		boolean chk = opcodes.isDirective(operation);
		if (!chk && !operation.contains("=")) {
			objcode1(operandLoc);
		} else {
			objcode2();
		}
	}

	private void objcode1(String operandLoc) {
		String res = opcodes.getCode(operation);
		if (operation.equals("rsub")) {
			res = "4F0000";
		} else if (operation.equals("+rsub")) {
			res = "4F000000";
		} else {
			// setting obcode for format 2
			if (opcodes.getFormatType(operation).equals("2")) {
				// setting obcode for format 2 with two registers
				if (operand.contains(",")) {
					String[] temp = operand.split(",");
					res += opcodes.getRegNum(temp[0]) + opcodes.getRegNum(temp[1]);
				}
				// setting obcode for format 2 with one register
				else {
					res += opcodes.getRegNum(operand) + "0";
				}
			}

			// setting obcode for format 3
			else if (opcodes.getFormatType(operation).equals("3")) {

				// check for immediate addressing
				if (operand.contains("#")) {
					String temp = operand.replace("#", "");
					if (isNumeric(temp)) {
						// n i x b p e
						// 0 1 0 0 0 0
						res = hxop.addHex(res, "1");
						res = makeRightCode(2, res.length(), res);
						res += "0";
						temp = hxop.toHex(temp);
						res += makeRightCode(3, temp.length(), temp);
					} else {
						// n i x b p e
						// 0 1 0 0 1 0
						res = hxop.addHex(res, "1");
						res = makeRightCode(2, res.length(), res);
						String dis = hxop.subHex(operandLoc, hxop.addHex(loc, "3"));
						dis = checkDis(dis);
						if (dis.length() > 3) {
							String baseAddress = getBaseLoc();
							dis = hxop.subHex(operandLoc, baseAddress);
							res += "4";
							res += makeRightCode(3, dis.length(), dis);
						} else {
							res += "2";
							res += makeRightCode(3, dis.length(), dis);
						}
					}
				}

				// check for indirect addressing
				else if (operand.contains("@")) {
					// n i x b p e
					// 1 0 0 0 1 0
					res = hxop.addHex(res, "2");
					res = makeRightCode(2, res.length(), res);
					String dis = hxop.subHex(operandLoc, hxop.addHex(loc, "3"));
					dis = checkDis(dis);
					if (dis.length() > 3) {
						String baseAddress = getBaseLoc();
						dis = hxop.subHex(operandLoc, baseAddress);
						res += "4";
						res += makeRightCode(3, dis.length(), dis);
					} else {
						res += "2";
						res += makeRightCode(3, dis.length(), dis);
					}
				}

				// check for indexing addressing
				else if (operand.contains(",") && operand.contains("x")) {
					// n i x b p e
					// 1 1 1 0 1 0
					res = hxop.addHex(res, "3");
					res = makeRightCode(2, res.length(), res);
					String dis = hxop.subHex(operandLoc, hxop.addHex(loc, "3"));
					dis = checkDis(dis);
					if (dis.length() > 3) {
						String baseAddress = getBaseLoc();
						dis = hxop.subHex(operandLoc, baseAddress);
						res += "C";
						res += makeRightCode(3, dis.length(), dis);
					} else {
						res += "A";
						res += makeRightCode(3, dis.length(), dis);
					}
				}

				// direct addressing
				else {
					// n i x b p e
					// 1 1 0 0 1 0
					res = hxop.addHex(res, "3");
					res = makeRightCode(2, res.length(), res);
					String dis = hxop.subHex(operandLoc, hxop.addHex(loc, "3"));
					dis = checkDis(dis);
					if (dis.length() > 3) {
						String baseAddress = getBaseLoc();
						dis = hxop.subHex(operandLoc, baseAddress);
						res += "4";
						res += makeRightCode(3, dis.length(), dis);
					} else {
						res += "2";
						res += makeRightCode(3, dis.length(), dis);
					}
				}
			}

			// setting obcode for format 4
			else {

				// check for immediate addressing
				if (operand.contains("#")) {
					String temp = operand.replace("#", "");
					if (isNumeric(temp)) {
						// n i x b p e
						// 0 1 0 0 0 1
						res = hxop.addHex(res, "1");
						res = makeRightCode(2, res.length(), res);
						res += "1";
						temp = hxop.toHex(temp);
						res += makeRightCode(5, temp.length(), temp);
					} else {
						// n i x b p e
						// 0 1 0 0 0 1
						res = hxop.addHex(res, "1");
						res = makeRightCode(2, res.length(), res);
						res += "1";
						res += makeRightCode(5, operandLoc.length(), operandLoc);
					}
				}

				// check for indirect addressing
				else if (operand.contains("@")) {
					// n i x b p e
					// 1 0 0 0 0 1
					res = hxop.addHex(res, "2");
					res = makeRightCode(2, res.length(), res);
					res += "1";
					res += makeRightCode(5, operandLoc.length(), operandLoc);
				}

				// check for indexing addressing
				else if (operand.contains(",") && operand.contains("x")) {
					// n i x b p e
					// 1 1 1 0 0 1
					res = hxop.addHex(res, "3");
					res = makeRightCode(2, res.length(), res);
					res += "9";
					res += makeRightCode(5, operandLoc.length(), operandLoc);
				}

				// direct addressing
				else {
					// n i x b p e
					// 1 1 0 0 0 1
					res = hxop.addHex(res, "3");
					res = makeRightCode(2, res.length(), res);
					res += "1";
					res += makeRightCode(5, operandLoc.length(), operandLoc);
				}
			}
		}
		setobjcode(res);
	}

	private void objcode2() {
		String res = "";
		if (operation.equals("word")) {
			String s = hxop.toHex(operand);
			res = "";
			for (int i = 0; i < 6 - s.length(); i++) {
				res += "0";
			}
			res += s;
			setobjcode(res);
		} else if (operation.equals("byte")) {
			if (operand.charAt(0) == 'X' || operand.charAt(0) == 'x') {
				int ln = operand.length();
				for (int i = 2; i < ln - 1; i++) {
					res += operand.charAt(i);
				}

			} else {
				int ln = operand.length();
				for (int i = 2; i < ln - 1; i++) {
					res += getAscii(operand.charAt(i));
				}
			}
			setobjcode(res);
		} else {
			if (operation.contains("=")) {
				if (operation.charAt(1) == 'x' || operation.charAt(1) == 'X') {
					String temp = operation.replace("=", "");
					int ln = temp.length();
					for (int i = 2; i < ln - 1; i++) {
						res += temp.charAt(i);
					}
				} else if (operation.charAt(1) == 'c' || operation.charAt(1) == 'C') {
					String temp = operation.replace("=", "");
					int ln = temp.length();
					for (int i = 2; i < ln - 1; i++) {
						res += getAscii(temp.charAt(i));
					}
				} else {
					String temp = operation.replace("=", "");
					String s = hxop.toHex(temp);
					res = "";
					for (int i = 0; i < 6 - s.length(); i++) {
						res += "0";
					}
					res += s;
				}
				setobjcode(res);
			} else {
				setobjcode(null);
			}
		}
	}

	private String makeRightCode(int tar, int length, String res) {
		String right = res;
		for (int i = tar; i > length; i--) {
			right = "0" + right;
		}
		return right;
	}

	private String checkDis(String dis) {
		String diss = dis;
		if (dis.charAt(0) == 'f') {
			for (int i = 0; i < dis.length() - 1; i++) {
				if (dis.charAt(i) == 'f' && dis.charAt(i + 1) == 'f' && diss.length() > 3) {
					diss = diss.substring(1);
				}
				if (dis.charAt(i + 1) != 'f') {
					break;
				}
			}
		}
		return diss;
	}

	private String getBaseLoc() {
		String address = "";
		try {
			File file = new File("baseRelativeAddress.txt");
			@SuppressWarnings("resource")
			Scanner in = new Scanner(file);
			while (in.hasNextLine()) {
				address = in.nextLine();
			}
			return address;
		} catch (FileNotFoundException e) {
			System.out.println("No Base relative addressing allowed");
			System.exit(0);
		}
		return address;
	}

	private String getAscii(char c) {
		int asc = (int) c;
		String s = String.valueOf(asc);
		return hxop.toHex(s);
	}

	public boolean isNumeric(String s) {
		return s != null && s.matches("[-+]?\\d*\\.?\\d+");
	}

	public boolean operationIsDirective() {
		return opcodes.isDirective(this.operation);
	}

	public void print(BufferedWriter bufferedWriter) throws IOException {
		if (allComment) {
			bufferedWriter.write(uppercase(comment));
			bufferedWriter.newLine();
			return;
		}
		while (loc.length() < 4) {
			loc = "0" + loc;
		}
		if(operation.equals("base") || operation.equals("nobase") || operation.equals("equ") || operation.equals("org") || operation.equals("ltorg")){
			loc = "    ";
		}
		bufferedWriter.write(loc + "    ");
		printLabel(uppercase(label), bufferedWriter);
		bufferedWriter.write(" ");
		printOp(uppercase(operation), bufferedWriter);
		bufferedWriter.write("  ");
		printOperand(uppercase(operand), bufferedWriter);
		bufferedWriter.write(uppercase(comment) + "     ");
		if (objcode != null)
			bufferedWriter.write(uppercase(objcode));
		bufferedWriter.newLine();
	}

	private void printLabel(String label, BufferedWriter bufferedWriter) throws IOException {
		int len;
		if (label == null)
			len = 0;
		else {
			bufferedWriter.write(label);
			len = label.length();
		}
		for (int i = 8 - len; i > 0; i--) {
			bufferedWriter.write(" ");
		}
	}

	private void printOp(String op, BufferedWriter bufferedWriter) throws IOException {
		int len;
		if (op == null)
			len = 0;
		else {
			bufferedWriter.write(op);
			len = op.length();
		}
		for (int i = 6 - len; i > 0; i--) {
			bufferedWriter.write(" ");
		}
	}

	private void printOperand(String operand, BufferedWriter bufferedWriter) throws IOException {
		int len;
		if (operand == null)
			len = 0;
		else {
			bufferedWriter.write(operand);
			len = operand.length();
		}
		for (int i = 18 - len; i > 0; i--) {
			bufferedWriter.write(" ");
		}
	}

	private String uppercase(String s) {
		if (Isemptystr(s))
			return s;
		String res = "";
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c >= 'a' && c <= 'z')
				c = Character.toUpperCase(c);
			res += c;
		}
		return res;
	}

	private boolean Isemptystr(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) != ' ')
				return false;
		}
		return true;
	}
}
