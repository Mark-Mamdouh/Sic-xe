import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

class node {
	public String loc;
	public String type;
//	public String length;

	public node(String loc, String type) {
		this.loc = loc;
		this.type = type;
//		this.length = length;
	}

	public String search(int index) {
		if (index == 1)
			return loc;
		else if (index == 2)
			return type;
//		else
//			return length;
		return null;
	}

}

public class Symtable {
	private HashMap<String, node> symtab;
//	private Hex hxdcop;

	public Symtable() {
		symtab = new HashMap<String, node>();
//		hxdcop = new Hex();
	}

	public boolean insert(String label, String loc, String type) {
		if (symtab.containsKey(label))
			return false;
		node n = new node(loc, type);
		symtab.put(label, n);
		return true;
	}

	public String search(String label, int index) {
		if (!symtab.containsKey(label))
			return null;
		String prop = symtab.get(label).search(index);
		return prop;
	}

	public boolean exist(String label) {
		return symtab.containsKey(label);
	}

//	public String SymtabLen() {
//		String len = "0000";
//		for (String k : symtab.keySet()) {
//			len = hxdcop.addHex(len, symtab.get(k).length);
//		}
//		return len;
//	}

	public void printSymtab(BufferedWriter bufferedWriter) throws IOException {
		String str = "";
		bufferedWriter.newLine();
		bufferedWriter.newLine();
		bufferedWriter.newLine();
		bufferedWriter.write("***********************SYMTAB***************************");
		bufferedWriter.newLine();
		for (String k : symtab.keySet()) {
			String loc = symtab.get(k).search(1);
			str = makeSpaces(k);
			bufferedWriter.write(k + str + loc);
			bufferedWriter.newLine();
		}
		bufferedWriter.write("***********************SYMTAB***************************");
	}

	private String makeSpaces(String k) {
		String sp = "";
		for (int i = k.length(); i < 15; i++) {
			sp += " ";
		}
		return sp;
	}
}
