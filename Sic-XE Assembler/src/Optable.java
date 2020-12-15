import java.util.List;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Optable {
	private HashMap<String, String> opcodes;
	private HashMap<String, String> formatType;
	private List<String> directive;
	private List<String> registers;

	public Optable() {
		opcodes = new HashMap<>();
		formatType = new HashMap<>();
		directive = new ArrayList<>();
		registers = new ArrayList<>();

		// put all opcodes from file opcode.txt into
		// hash table<string, string> ==> <opcode, value in hex>
		readOpCodes();

		// put all our known directives in an array list
		readDirective();

		setRegisters();

	}

	private void readOpCodes() {
		try {
			@SuppressWarnings("resource")
			Scanner in = new Scanner(new File("optable.txt"));
			while (in.hasNextLine()) {
				String line = in.nextLine();
				String[] words = line.split(" ");
				opcodes.put(words[0], words[1]);
				formatType.put(words[0], words[2]);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void readDirective() {
		directive.add("start");
		directive.add("end");
		directive.add("resb");
		directive.add("resw");
		directive.add("word");
		directive.add("byte");
		directive.add("org");
		directive.add("equ");
		directive.add("base");
		directive.add("nobase");
		directive.add("ltorg");
	}

	private void setRegisters() {
		registers.add("a");
		registers.add("x");
		registers.add("l");
		registers.add("b");
		registers.add("s");
		registers.add("t");
		registers.add("f");
	}

	public boolean isDirective(String mnemonic) {
		return directive.contains(mnemonic);
	}

	public boolean IsExist(String mnemonic) {
		return opcodes.containsKey(mnemonic) || directive.contains(mnemonic);
	}

	public String getCode(String mnemonic) {
		if (!IsExist(mnemonic))
			throw new RuntimeException("Error this word is invalid");
		return opcodes.get(mnemonic);
	}

	public String getFormatType(String mnemonic) {
		if (!IsExist(mnemonic))
			throw new RuntimeException("Error this word is invalid");
		return formatType.get(mnemonic);
	}

	public boolean isRegister(String reg) {
		return registers.contains(reg);
	}

	public String getRegNum(String reg) {
		if (reg.equals("a")) {
			return "0";
		} else if (reg.equals("x")) {
			return "1";
		} else if (reg.equals("l")) {
			return "2";
		} else if (reg.equals("b")) {
			return "3";
		} else if (reg.equals("s")) {
			return "4";
		} else if (reg.equals("t")) {
			return "5";
		} else if (reg.equals("f")) {
			return "6";
		}
		return null;
	}
}
