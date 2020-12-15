import java.util.List;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;


public class Optable {
       private HashMap<String , String> opcodes; 
       private List<String> directive ;
       private Hex hx ;
       public Optable(){
    	   opcodes = new HashMap<>();
    	   directive = new ArrayList<>();
    	   hx = new Hex(); 
    	   
    	   //put all opcodes from file opcode.txt into 
           //hash table<string, string> ==> <opcode, value in hex>
           readOpCodes();
         
           //put all our known directives in an array list
    	   readDirective();
    	   
       }
       
       private void readOpCodes(){
    	     try {
				@SuppressWarnings("resource")
				Scanner in = new Scanner(new File("optable.txt"));
				while(in.hasNextLine()){
					 String line = in.nextLine();
					 String[] words = line.split(" ");
					 String hxval = hx.toHex(words[1]);
					 if(hxval.length() == 1 ) hxval = "0"+hxval ;
					 opcodes.put(words[0],hxval);
				}
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
       }
       
       private void readDirective(){
    	   directive.add("start");
    	   directive.add("end");
    	   directive.add("resb");
    	   directive.add("resw");
    	   directive.add("word");
    	   directive.add("byte");
       }
       
       public boolean isDirective(String mnemonic){
    	      return  directive.contains(mnemonic);
       }
       
       public boolean IsExist(String mnemonic){
    	      return opcodes.containsKey(mnemonic)||directive.contains(mnemonic);
       }
       
       public String getCode(String mnemonic){
    	     if(!IsExist(mnemonic)) throw new RuntimeException("Error this word is invalid");
    	     return opcodes.get(mnemonic);
       }
}
