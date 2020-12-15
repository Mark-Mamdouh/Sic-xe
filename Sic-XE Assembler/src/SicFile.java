import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class SicFile {
	public List<Line> file;
	public File sourceFile;
	public File errorFile;
	public Scanner in;
	public Optable opcodes;
	public String locCounter;
	public String progname;
	public String startloc, endloc, proglen;
	public Symtable symtab;
	public Hex hxdcop;
	public List<String> finalObjCode;
	public List<String> allLabels;
	public HashMap<String, String> allLiterals;
	public List<String> literals;
	public boolean eof;
	public boolean stof;
	public boolean error;

	FileWriter erroWriter;
	BufferedWriter errorbufferWriter;

	public SicFile(Optable opcodes) {
		error = false;
		locCounter = "0000";
		proglen = "0000";
		progname = null;
		eof = false;
		stof = false;
		file = new ArrayList<>();
		symtab = new Symtable();
		hxdcop = new Hex();
		finalObjCode = new ArrayList<>();
		allLabels = new ArrayList<>();
		allLiterals = new HashMap<>();
		literals = new ArrayList<>();
		sourceFile = new File("source2.txt");
		errorFile = new File("errors.txt");

		try {
			erroWriter = new FileWriter("errors.txt");
			errorbufferWriter = new BufferedWriter(erroWriter);

			in = new Scanner(sourceFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.opcodes = opcodes;
	}

	public void read() {

		while (in.hasNextLine()) {
			String line = in.nextLine();

			// check for empty line
			if (line.length() == 0)
				continue;
			// replace tabs with spaces
			line = line.replaceAll("\t", "    ");
			// check for comment line
			if (line.charAt(0) == '.') {
				Line ins = new Line(null, null, null, null, null, null);
				ins.comment = line;
				ins.allComment = true;
				file.add(ins);
				continue;
			}

			// make string array of size 4
			// for label, operation, operand, loccont and format fields

			// TODO REGEX

			String[] words = new String[5];
			words[0] = substr(line, 0, 7);
			words[0] = lowercase(words[0]);
			words[1] = substr(line, 9, 14);
			words[1] = lowercase(words[1]);
			words[2] = substr(line, 17, 34);
			// remove spaces
			words[0] = words[0].trim();
			words[1] = words[1].trim();
			words[2] = words[2].trim();
			// make operand lower case if operation is not byte
			// because we don't want to change value of hexadecimal or character
			if (!words[1].equals("byte") && !words[2].contains("=")) {
				words[2] = lowercase(words[2]);
			}
			words[3] = substr(line, 35, 65);
			words[3] = words[3].trim();

			words[4] = opcodes.getFormatType(words[1]);

			allLabels.add(words[0]);

			if (!words[1].equals("rsub") && !words[1].equals("nobase") && !words[1].equals("ltorg")) {
				if (words[2].charAt(0) == '=' && !literals.contains(words[2])) {
					literals.add(words[2]);
				}
			}

			// check for all errors and return true if there is no error
			boolean ch1 = checkLine(words);
			if (!ch1) {
				error = true;
			}
		}

		// check for existence of start and end
		if (stof == false || eof == false) {
			writeError("Source code not valid");
		}

		// make symtab
		fill_symtab();

		// check if operand is allowed
		checkAllOperands();

		if (!error) {

			// set relative base address
			setBaseRelativeAddress();

			// set object code for every line
			setobjcodes();

			// make lisfile.txt
			printinsts();

			// make objfile.txt
			collectobjCodes();

			File file = new File("baseRelativeAddress.txt");
			file.delete();
		}
		try {
			errorbufferWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean checkLine(String[] words) {
		String flabel = words[0], ftype = words[1], foperand = words[2], currloc = locCounter;

		// check for existence of operation
		if (!checktype(ftype)) {
			writeError("Error Operation not supported or doesn't exist");
			return false;
		}

		if (ftype.equals("start")) {
			// check if there is more than one start of our program
			if (stof) {
				writeError("Error there are more than one start in the source code");
				return false;
			}
			// check if there is no label and start is not a hex value
			if (Isemptystr(words[0]) || (!hxdcop.IsHex(words[2]))) {
				writeError("Error in Start line: there is no label or error in address");
				return false;
			}
			stof = true;
		}

		// check for end of file
		if (ftype.equals("end")) {
			if (eof) {
				writeError("Error there are more than one start in the source code");
				return false;
			}
			eof = true;
		}

		// check for integer value after word, resb, resw
		if (ftype.equals("word") || ftype.equals("resw") || ftype.equals("resb")) {
			boolean chk = isInt(foperand);
			if (!chk) {
				writeError("Error there is no decimal value after word, resw or resb");
				return false;
			}
		}

		// check for x or c after byte operation
		if (ftype.equals("byte")) {
			if (foperand.charAt(0) == 'X' || foperand.charAt(0) == 'C' || foperand.charAt(0) == 'x'
					|| foperand.charAt(0) == 'c') {
			} else {
				writeError("Error in operand of byte directive");
				return false;
			}
		}

		// check for empty label and operand for rsub
		if (ftype.equals("rsub")) {
			if (Isemptystr(words[0]) && Isemptystr(words[2])) {
			} else {
				writeError("Error in rsub: there is a label or operand");
				return false;
			}
		}

		// check for registers in format 2
		if (!opcodes.isDirective(words[1])) {
			if (words[4].equals("2")) {
				if (words[2].contains(",")) {
					String[] temp = words[2].split(",");
					if (temp.length != 2) {
						writeError(
								"Error in a format 2 line: There is one register and , or there are more than two registers");
						return false;
					}
					if (!opcodes.isRegister(temp[0])) {
						writeError("Error in a format 2 line: register 1 not supported");
						return false;
					}
					if (!opcodes.isRegister(temp[1])) {
						writeError("Error in a format 2 line: register 2 not supported");
						return false;
					}
				} else {
					if (!opcodes.isRegister(words[2])) {
						writeError("Error in a format 2 line: register not supported");
						return false;
					}
				}
			}
		}

		// check for org operation
		if (words[1].equals("org")) {
			if (!words[0].isEmpty()) {
				writeError("Error in an ORG statement: there is a label");
				return false;
			}
			if (words[2].isEmpty()) {
				writeError("Error in an ORG statement: there is no operand");
				return false;
			}
			if (!isNumeric(words[2])) {
				String st = words[2];
				st = st.replace("@", "");
				st = st.replace("#", "");
				if (!allLabels.contains(st)) {
					writeError("Error in an ORG statement: Undefined Symbol");
					return false;
				}
			}
		}

		// check for equ operation
		if (words[1].equals("equ")) {
			if (words[0].isEmpty()) {
				writeError("Error in an EQU statement: there is no label");
				return false;
			}
			if (words[2].isEmpty()) {
				writeError("Error in an EQU statement: there is no operand");
				return false;
			}
			if (!isNumeric(words[2])) {
				// TODO
			}
		}

		// check for base operation
		if (words[1].equals("base")) {
			if (!words[0].isEmpty()) {
				writeError("Error in a BASE statement: there is a label");
				return false;
			}
			if (words[2].isEmpty()) {
				writeError("Error in a Base statement: there is no operand");
				return false;
			}
		}

		// check for nobase operation
		if (words[1].equals("nobase")) {
			if (!words[0].isEmpty()) {
				writeError("Error in an NOBASE statement: there is a label");
				return false;
			}
			if (!words[2].isEmpty()) {
				writeError("Error in an NOBASE statement: there is an operand");
				return false;
			}
		}

		// check for ltorg operation
		if (words[1].equals("ltorg")) {
			if (!words[0].isEmpty()) {
				writeError("Error in an LTORG statement: there is a label");
				return false;
			}
			if (!words[2].isEmpty()) {
				writeError("Error in an LTORG statement: there is an operand");
				return false;
			}
		}

		// check literals
		if (!words[1].equals("rsub") && !words[1].equals("nobase") && !words[1].equals("ltorg")) {
			if (words[2].charAt(0) == '=') {
				if (words[2].charAt(1) != '\'' && words[2].charAt(1) != 'X' && words[2].charAt(1) != 'C') {
					writeError("Error: Wrong literal format");
					return false;
				}
				if (!allLiterals.containsKey(words[2])) {
					allLiterals.put(words[2], "");
				}
			}
		}

		// Increase location counter
		if (!ftype.equals("ltorg")) {
			if (ftype.equals("start")) {
				progname = flabel;
				startloc = foperand;
				locCounter = startloc;
				currloc = locCounter;
			} else if (ftype.equals("word")) {
				locCounter = hxdcop.addHex(locCounter, "3");
			} else if (ftype.equals("byte")) {
				char fc = foperand.charAt(0);
				if (fc == 'X' || fc == 'x')
					locCounter = hxdcop.addHex(locCounter, "1");
				else {
					int len = foperand.length() - 3;
					locCounter = hxdcop.addHex(locCounter, hxdcop.toHex(String.valueOf(len)));
				}
			} else if (ftype.equals("resw")) {
				int len = 3 * Integer.parseInt(foperand);
				locCounter = hxdcop.addHex(locCounter, hxdcop.toHex(String.valueOf(len)));
			} else if (ftype.equals("resb")) {
				int len = Integer.parseInt(foperand);
				locCounter = hxdcop.addHex(locCounter, hxdcop.toHex(String.valueOf(len)));
			} else if (ftype.equals("end")) {
				eof = true;
			} else if (!ftype.equals("org") && !ftype.equals("equ") && !ftype.equals("base") && !ftype.equals("nobase")
					&& !ftype.equals("ltorg")) {
				locCounter = hxdcop.addHex(locCounter, opcodes.getFormatType(ftype));
			}
			endloc = currloc;
			Line ins = new Line(currloc, flabel, ftype, foperand, null, opcodes);
			ins.comment = words[3];
			file.add(ins);
			if (ftype.equals("end")) {
				while (literals.size() != 0) {
					String lit = literals.get(0);
					lit = lit.replace("=", "");
					if (lit.charAt(0) == '\'') {
						locCounter = hxdcop.addHex(locCounter, "3");
					} else if (lit.charAt(0) == 'x' || lit.charAt(0) == 'X') {
						locCounter = hxdcop.addHex(locCounter, "1");
					} else {
						int len = lit.length() - 3;
						locCounter = hxdcop.addHex(locCounter, hxdcop.toHex(String.valueOf(len)));
					}
					endloc = currloc;
					ins = new Line(currloc, "*", literals.get(0), "", null, opcodes);
					allLiterals.replace(literals.get(0), "", currloc);
					ins.comment = "";
					file.add(ins);
					literals.remove(literals.get(0));
				}
			}
			return true;
		}

		// handling ltorg
		else {
			endloc = currloc;
			Line ins = new Line(currloc, flabel, ftype, foperand, null, opcodes);
			ins.comment = words[3];
			file.add(ins);
			while (literals.size() != 0) {
				String lit = literals.get(0);
				lit = lit.replace("=", "");
				if (lit.charAt(0) == '\'') {
					locCounter = hxdcop.addHex(locCounter, "3");
				} else if (lit.charAt(0) == 'x' || lit.charAt(0) == 'X') {
					locCounter = hxdcop.addHex(locCounter, "1");
				} else {
					int len = lit.length() - 3;
					locCounter = hxdcop.addHex(locCounter, hxdcop.toHex(String.valueOf(len)));
				}
				endloc = currloc;
				ins = new Line(currloc, "*", literals.get(0), "", null, opcodes);
				allLiterals.replace(literals.get(0), "", currloc);
				ins.comment = "";
				file.add(ins);
				literals.remove(literals.get(0));
			}
		}
		return true;
	}

	private void checkAllOperands() {
		for (Line inst : file) {
			if (inst.allComment)
				continue;
			if (inst.operationIsDirective())
				continue;
			String st = inst.operand;
			if (st.length() == 0)
				continue;
			String stt = st.split(",")[0];
			stt = stt.replace("#", "");
			if (isNumeric(stt)) {
				continue;
			}
			if(inst.operand.contains("=")){
				continue;
			}
			stt = stt.replace("@", "");
			if (!symtab.exist(stt) && !opcodes.isRegister(stt)) {
				writeError(stt + " doesn't exist in symtab");
				error = true;
			}
		}
	}

	private void fill_symtab() {
		List<String> locs = new ArrayList<>();
		for (Line inst : file) {
			if (inst.allComment)
				continue;
			if (inst.label.length() != 0)
				locs.add(inst.loc);
		}
		locs.add(endloc);
		int i = 1;
		for (Line inst : file) {
			if (inst.allComment)
				continue;
			if (inst.label.length() != 0 && !inst.label.equals("*")) {
				String diff = hxdcop.subHex(locs.get(i), inst.loc);
				i++;
				boolean chk = symtab.insert(inst.label, inst.loc, inst.operation, diff);
				if (!chk) {
					writeError(inst.label + "  already exists ");
					error = true;
				}
			}
		}
	}

	public boolean isNumeric(String s) {
		return s != null && s.matches("[-+]?\\d*\\.?\\d+");
	}

	private void collectobjCodes() {
		proglen = symtab.SymtabLen();
		String head = "";
		head += "H^" + progname;
		for (int i = 0; i < 6 - progname.length(); i++) {
			head += " ";
		}
		head += "^";
		head += fitstr(startloc, 6) + "^";
		head += fitstr(proglen, 6);
		finalObjCode.add(head);

		String loc = null;
		int recordlen = 0;

		List<String> record = new ArrayList<>();
		for (int i = 0; i < file.size(); i++) {
			Line ins = file.get(i);
			if (ins.allComment)
				continue;
			if (loc == null)
				loc = ins.loc;
			if (ins.operation.equals("resw") || ins.operation.equals("resb")) {
				if (record.size() != 0) {
					String rec = getRecord(record, recordlen / 2, loc);
					finalObjCode.add(rec);
				}
				loc = null;
				recordlen = 0;
				record.clear();
				continue;
			}
			String objc = ins.objcode;
			if (ins.operation.equals("end")) {
				String rec = getRecord(record, recordlen / 2, loc);
				finalObjCode.add(rec);
			}
			if (objc == null)
				continue;
			if (objc.length() + recordlen > 60) {
				String rec = getRecord(record, recordlen / 2, loc);
				finalObjCode.add(rec);
				record.clear();
				loc = ins.loc;
				recordlen = 0;

			}
			recordlen += objc.length();
			record.add(objc);

		}
		// Modification Record
		for (int i = 0; i < file.size(); i++) {
			Line ins = file.get(i);
			if (ins.operation.contains("+")) {
				if (ins.operand.contains("#")) {
					String[] temp = ins.operand.split("#");
					if (isNumeric(temp[1])) {
						continue;
					}
				}
				String mod = "M^";
				String modLoc = hxdcop.addHex(ins.loc, "1");
				while (modLoc.length() < 6) {
					modLoc = "0" + modLoc;
				}
				mod += modLoc + "^05";
				finalObjCode.add(mod);

			}

		}
		String tale = "E^" + fitstr(startloc, 6);
		finalObjCode.add(tale);

		String fileName = "objfile.txt";
		try {
			FileWriter fileWriter = new FileWriter(fileName);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			for (String s : finalObjCode) {
				bufferedWriter.write(uppercase(s));
				bufferedWriter.newLine();
			}
			bufferedWriter.close();
		} catch (IOException ex) {
			System.out.println("Error writing to file '" + fileName + "'");
		}
	}

	private String getRecord(List<String> objcodes, int len, String stloc) {
		String res = "T^";
		res += fitstr(stloc, 6) + "^";
		String hxln = hxdcop.toHex(String.valueOf(len));
		res += fitstr(hxln, 2);
		for (String s : objcodes) {
			res += "^" + s;
		}
		return res;
	}

	private String fitstr(String s, int num) {
		String res = "";
		for (int i = 0; i < num - s.length(); i++) {
			res += "0";
		}
		res += s;
		return res;
	}

	private void setobjcodes() {
		for (Line inst : file) {
			if (inst.allComment)
				continue;
			if (inst.operationIsDirective()) {
				inst.calObjcode(null);
			} else {
				String op = inst.operand.split(",")[0];
				op = op.replace("#", "");
				op = op.replace("@", "");
				if (op.contains("=") && !inst.operation.contains("=")) {
					String oploc = allLiterals.get(inst.operand);
					inst.calObjcode(oploc);
				} else if (inst.operation.contains("=")) {
					String oploc = allLiterals.get(inst.operation);
					inst.calObjcode(oploc);
				} else {
					String oploc = symtab.search(op, 1);
					inst.calObjcode(oploc);
				}
			}
		}
	}

	private void setBaseRelativeAddress() {
		boolean astricFound = false;
		for (Line inst : file) {
			if (inst.operation.equals("base")) {
				if (inst.operand.equals("*") && !astricFound) {
					astricFound = true;
					continue;
				} else if (astricFound) {
					try {
						PrintWriter writer = new PrintWriter("baseRelativeAddress.txt");
						writer.print(inst.loc);
						writer.close();
					} catch (IOException e) {
					}
					return;
				} else {
					String op = inst.operand.split(",")[0];
					op = op.replace("#", "");
					op = op.replace("@", "");
					String oploc = symtab.search(op, 1);
					try {
						PrintWriter writer = new PrintWriter("baseRelativeAddress.txt");
						writer.print(oploc);
						writer.close();
					} catch (IOException e) {
					}
				}
			}
		}
	}

	private void printinsts() {
		String fileName = "lisfile.txt";
		try {
			FileWriter fileWriter = new FileWriter(fileName);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			for (Line ints : file) {
				ints.print(bufferedWriter);
			}
			symtab.printSymtab(bufferedWriter);
			bufferedWriter.newLine();
			bufferedWriter.newLine();
			bufferedWriter.newLine();
			bufferedWriter.write("***********************LITTAB***************************");
			bufferedWriter.newLine();
			Iterator<?> it = allLiterals.entrySet().iterator();
			while (it.hasNext()) {
				@SuppressWarnings("rawtypes")
				Map.Entry pair = (Map.Entry) it.next();
				String key = pair.getKey().toString();
				key = key.replaceAll("'", "");
				key = key.replace("X", "");
				key = key.replace("C", "");
				key = key.replace("=", "");
				String hxValue = "";
				if (hxdcop.IsHex(key)) {
					hxValue = hxdcop.toHex2(key);
				} else {
					hxValue = hxdcop.charToHex(key);
				}
				bufferedWriter.write((pair.getKey() + makeSpaces(pair.getKey().toString()) + hxValue
						+ makeSpaces(hxValue) + hxValue.length() / 2 + makeSpaces(String.valueOf(hxValue.length() / 2))
						+ pair.getValue()));
				bufferedWriter.newLine();
				it.remove(); // avoids a ConcurrentModificationException
			}
			bufferedWriter.write("***********************LITTAB***************************");
			bufferedWriter.close();
		} catch (IOException ex) {
			System.out.println("Error writing to file '" + fileName + "'");
		}
	}

	private static String makeSpaces(String k) {
		String sp = "";
		for (int i = k.length(); i < 15; i++) {
			sp += " ";
		}
		return sp;
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

	private String lowercase(String s) {
		if (Isemptystr(s))
			return s;
		String res = "";
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c >= 'A' && c <= 'Z')
				c = Character.toLowerCase(c);
			res += c;
		}
		return res;
	}

	private String substr(String s, int i1, int i2) {
		String res = "";
		for (int i = i1; i <= i2 && i < s.length(); i++) {
			res += s.charAt(i);
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

	private boolean checktype(String type) {
		return opcodes.IsExist(type);
	}

	private boolean isInt(String st) {
		try {
			Integer.parseInt(st);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private void writeError(String s) {
		try {
			errorbufferWriter.write(s);
			errorbufferWriter.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
