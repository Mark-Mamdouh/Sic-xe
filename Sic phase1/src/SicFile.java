import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class SicFile {
      public List<Line> file ;
      public File sourceFile;
      public File errorFile;
      public Scanner in;
      public Optable opcodes ;
      public String locCounter ;
      public String progname ;
      public String startloc , endloc , proglen;
      public Symtable symtab ;
      public Hex hxdcop ;
      public List<String> finalObjCode;
      public boolean eof ;
      public boolean stof ;
      public boolean error ;
      
      FileWriter erroWriter ;
      BufferedWriter errorbufferWriter ;
      
      public SicFile(Optable opcodes){
    	     error = false ;
    	     locCounter = "0000" ; 
    	     proglen = "0000";
    	     progname = null ;
    	     eof = false;
    	     stof = false ;
    	     file = new ArrayList<>();
    	     symtab = new Symtable();
    	     hxdcop = new Hex();
    	     finalObjCode = new ArrayList<>();
    	     sourceFile = new File("source.txt");
    	     errorFile = new File("errors.txt");
    	     
    	     try {
    	    	   erroWriter = new FileWriter("errors.txt");
    	    	   errorbufferWriter = new BufferedWriter(erroWriter);
    	    	   
				   in = new Scanner(sourceFile);
			     } catch (FileNotFoundException e) {
				 // TODO Auto-generated catch block
				 e.printStackTrace();
			 } catch (IOException e) {
			     // TODO Auto-generated catch block
				 e.printStackTrace();
			 }
    	     
    	     this.opcodes = opcodes;
      }
      
      public void read(){
    	      while(in.hasNextLine()){
    	    	     String line = in.nextLine();
                 //check for empty line
    	    	     if(line.length() == 0) continue ;
                 //replace tabs with spaces
    	    	     line = line.replaceAll("\t","    ");
                 //check for comment line
    	    	     if(line.charAt(0)=='.'){
    	    	    	   Line ins = new Line(null , null , null , null , null , null);
    	    	    	   ins.comment = line;
    	    	    	   ins.allComment = true;
    	    	    	   file.add(ins);
    	    	    	   continue ;
    	    	     }
                 //make string array of size 4
                 //for label, operation, operand and loccont fields
    	    	     String[] words = new String[4];
    	    	     words[0] = substr(line , 0 , 7); words[0] = lowercase(words[0]);
    	    	     words[1] = substr(line , 9 , 14);words[1] = lowercase(words[1]);
    	    	     words[2] = substr(line , 17 , 34);
                 //remove spaces
    	    	     words[0] = words[0].trim();
    	    	     words[1] = words[1].trim();
    	    	     words[2] = words[2].trim();
                 //make operand lower case if operation is not byte
                 //because we don't want to change value of hexadecimal or character
    	    	     if(!words[1].equals("byte")){
    	    	    	  words[2] = lowercase(words[2]);
    	    	     }
    	    	     words[3] = substr(line , 35 , 65);
    	    	     words[3] = words[3].trim();
    	    	     
                 //check for all errors and return true if there is no error
    	    	     boolean ch1 = checkLine(words);
    	    	     if(!ch1){
    	   
    	    	    	 error = true ;
    	    	     }
    	      }
    	      
            //check for existence of start and end
    	      if(stof==false || eof==false){
    	    	  throw new RuntimeException("Source code not valid");
    	      }
    	      
            //make symtab
    	      fill_symtab();

            //check if operand is allowed
    	      checkAllOperands();
    	      
    	     if(!error){
               //set object code for every line
    	         setobjcodes();

               //make sicfile.txt
    	         printinsts();

               //make objfile.txt
    	         collectobjCodes();
    	     }
    	     try {
				errorbufferWriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
      }
      
      
      private void collectobjCodes(){
    	      proglen = symtab.SymtabLen() ;
    	      String head = "" ;
    	      head += "H^"+progname ;
    	      for(int i=0;i<6-progname.length();i++){
    	    	   head +=" ";
    	      } head +="^" ;
    	      head += fitstr(startloc,6)+"^";
    	      head += fitstr(proglen,6);
    	      finalObjCode.add(head);
    	      
    	      String loc = null ;
    	      int recordlen = 0 ;
    	      
    	      List<String> record = new ArrayList<>();
    	      for(int i=0; i<file.size() ; i++){
    	    	      Line ins = file.get(i);
    	    	      if(ins.allComment) continue;
    	    	      if(loc == null) loc = ins.loc;
    	    	      if(ins.operation.equals("resw") || ins.operation.equals("resb")){
    	    	    	  if(record.size() != 0){
    	    	    		  String rec = getRecord(record , recordlen/2 , loc);
    	    	    		  finalObjCode.add(rec);
    	    	    	  }
    	    	    	  loc = null ;
    	    	    	  recordlen = 0 ;
    	    	    	  record.clear();
    	    	    	  continue ;
    	    	      }
    	    	      String objc = ins.objcode;
    	    	      if(ins.operation.equals("end")){
    	    	    	  String rec = getRecord(record , recordlen/2 , loc);
	    	    		  finalObjCode.add(rec);
    	    	      }
    	    	      if(objc == null) continue;
    	    	      if(objc.length() + recordlen > 60){
    	    	    	  String rec = getRecord(record , recordlen/2 , loc);
	    	    		  finalObjCode.add(rec);
	    	    		  record.clear();
	    	    		  loc = ins.loc;
	    	    		  recordlen =0;
	    	    		 
    	    	      }
    	    	      recordlen += objc.length() ;
    	    	      record.add(objc);
    	    	      
    	      }
    	      
    	     String tale = "E^"+fitstr(startloc,6);
    	     finalObjCode.add(tale);
    	     
    	     
    	   String fileName = "result.txt";
      	   try{
      		   FileWriter fileWriter = new FileWriter(fileName);
      	       BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
      	         for(String s : finalObjCode){
      	    	    bufferedWriter.write(uppercase(s));
      	    	    bufferedWriter.newLine();
      	         }
      	       bufferedWriter.close();
      	   }catch(IOException ex) {
                 System.out.println("Error writing to file '" + fileName + "'");
            }
      }
      
      private String getRecord(List<String> objcodes , int len , String stloc){
    	     String res = "T^" ;
    	     res += fitstr(stloc,6)+"^";
    	     String hxln = hxdcop.toHex(String.valueOf(len)) ;
    	     res += fitstr(hxln,2);
    	     for(String s : objcodes){
    	    	   res += "^"+s ;
    	     }
    	     return res;
      }
      
      
      private String fitstr(String s , int num){
    	    String res = "";
    	    for(int i=0 ; i< num-s.length(); i++){
    	    	  res += "0";
    	    }
    	    res += s ;
    	    return res ;
      }
      
      
      private void setobjcodes(){
    	    for(Line inst : file){
    	    	if(inst.allComment) continue;
    	    	if(inst.operationIsDirective()){
    	    		 inst.calObjcode(null);
    	    	}else{
    	    		String op = inst.operand.split(",")[0] ;
    	    		String oploc = symtab.search(op, 1) ;
    	    		 inst.calObjcode(oploc);
    	    	}
    	    }
      }
      
      private void printinsts(){
    	   String fileName = "sicfile.txt";
    	   try{
    		   FileWriter fileWriter = new FileWriter(fileName);
    	       BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
    	         for(Line ints : file){
    		           ints.print(bufferedWriter);
    	         }
    	       symtab.printSymtab(bufferedWriter);
    	       bufferedWriter.close();
    	   }catch(IOException ex) {
               System.out.println("Error writing to file '"+ fileName + "'");
           }
      }
      
      
      private String uppercase(String s){
     	   if(Isemptystr(s)) return s ;
     	   String res = "" ;
     	   for(int i=0; i<s.length() ; i++){
     		   char c = s.charAt(i);
     		   if(c >= 'a' && c <= 'z') c = Character.toUpperCase(c);
     		   res += c ;
     	   }
     	   return res ;
       }
      
      private String lowercase(String s){
    	   if(Isemptystr(s)) return s ;
    	   String res = "" ;
    	   for(int i=0; i<s.length() ; i++){
    		   char c = s.charAt(i);
    		   if(c >= 'A' && c <= 'Z') c = Character.toLowerCase(c);
    		   res += c ;
    	   }
    	   return res ;
      }
      
      private String substr(String s , int i1 , int i2){
    	     String res = "";
    	     for(int i = i1 ; i<=i2&&i<s.length() ; i++){
    	    	  res += s.charAt(i);
    	     }
    	     return res ;
      }
      
      private boolean Isemptystr(String s){
    	    for(int i=0; i<s.length() ; i++){
    	    	 if(s.charAt(i) != ' ') return false ;
    	    }
    	    return true ;
      }
     
      
      private void checkAllOperands(){
    	   for(Line inst : file){
    		     if(inst.allComment) continue;
    		     if(inst.operationIsDirective()) continue;
    		     String st = inst.operand;
    		     if(st.length() == 0) continue;
    		     String stt = st.split(",")[0];
    		     if(!symtab.exist(stt)){
    		    	   writeError(stt + " doesn't exist in symtab");
    		    	   error = true ;   		     }
    	   }
      }
      
      
      private void fill_symtab(){
    	  List<String> locs  = new ArrayList<>();
    	  for(Line inst : file){
    		    if(inst.allComment) continue;
    		    if(inst.label.length() != 0 ) locs.add(inst.loc);
    	  }
    	  locs.add(endloc);
    	  int i=1 ;
    	  for(Line inst : file){
    		  if(inst.allComment) continue;
    		  if(inst.label.length() != 0 ){
    			    String diff = hxdcop.subHex(locs.get(i) , inst.loc);
    			    i++ ;
    			    boolean chk = symtab.insert(inst.label, inst.loc, inst.operation, diff);
  			        if(!chk){
  			        	writeError(inst.label + "  already exists ");
  			    	  error = true ;
  			      }
    		  }
    	  }
      }
      
      
      private boolean checkLine(String[] words){
    	   String flabel=words[0],ftype=words[1],foperand=words[2],currloc=locCounter;
    	   
         //check for existence of operation
    	   if(!checktype(ftype)){
    		   System.out.println("Error 1");
    		   return false ;
    	   }
    	   
    	   if(ftype.equals("start")){
          //check if there is more than one start of our program
    		       if(stof){
    		    	   System.out.println("Error 2");
    		    	   return false;
    		       }
               //check if there is no label and start is not a hex value
    		       if(Isemptystr(words[0]) || (!hxdcop.IsHex(words[2]))){
    		    	   System.out.println("Error 3");
    		    	    return false ;
    		       }
    		       stof = true ;
    	   }
    	   
         //check for end of file
    	   if(ftype.equals("end")){
    		     if(eof){
    		    	 System.out.println("Error 4");
    		    	 return false;
    		     }
    		     eof = true ;
    	   }
    		   
    	   //check for integer value after word, resb, resw
    	   if(ftype.equals("word") || ftype.equals("resw") || ftype.equals("resb")){
    		      boolean chk = isInt(foperand);
    		      if(!chk){
    		    	  System.out.println("Error 5");
    		    	  return false ;
    		      }
    	   }
    	   
         //check for x or c after byte operation
    	   if(ftype.equals("byte")) {
    		      if(foperand.charAt(0)=='X' || foperand.charAt(0)=='C' || foperand.charAt(0)=='x' || foperand.charAt(0)=='c'){}
    		      else {
    		    	  System.out.println("Error 6");
    		    	  return false ;
    		      }
    	   }
    	   
         //check for empty label and operand for rsub 
    	   if(ftype.equals("rsub")){
    		    if(Isemptystr(words[0]) && Isemptystr(words[2])) {}
    		    else {
    		    	System.out.println("Error 7");
    		    	return false ;
    		    }
    	   }
    	   
         //Increase location counter
    	   if(ftype.equals("start")){
    		   progname = flabel ;
    		   startloc = foperand ;
    		   locCounter = startloc ;
    		   currloc = locCounter ;
    	   }else if(ftype.equals("word")){
    		   locCounter = hxdcop.addHex(locCounter, "3") ;
    	   }else if(ftype.equals("byte")){
    		   char fc = foperand.charAt(0);
    		   if(fc == 'X' || fc == 'x') locCounter = hxdcop.addHex(locCounter, "1") ;
    		   else {
    			   int len = foperand.length()-3 ;
    			   locCounter = hxdcop.addHex(locCounter, hxdcop.toHex(String.valueOf(len))) ;
    		   }
    	   }else if(ftype.equals("resw")){
    		     int len = 3*Integer.parseInt(foperand);
    		     locCounter = hxdcop.addHex(locCounter, hxdcop.toHex(String.valueOf(len))) ;
    	   }else if(ftype.equals("resb")){
  		         int len = Integer.parseInt(foperand);
  		         locCounter = hxdcop.addHex(locCounter, hxdcop.toHex(String.valueOf(len))) ;
  	      }else if(ftype.equals("end")){ eof = true ;}
  	       else  locCounter = hxdcop.addHex(locCounter, "3") ;
    	   
    	   endloc = currloc ;
    	   Line ins = new Line(currloc , flabel , ftype , foperand , null , opcodes);
    	   ins.comment = words[3];
    	   String[] registerchk = foperand.split(",");
    	   if(registerchk.length > 1) ins.register = true;
    	   file.add(ins);
    	   return true ;
    	   
      }
      
      private boolean checktype(String type){
    	   return opcodes.IsExist(type);
      }
      
      private boolean isInt(String st){
    	   try{
    		   Integer.parseInt(st);
    		   return true ;
    	   }catch(NumberFormatException e){
    		   return false ;
    	   }
      }
      
      private void writeError(String s){
    	  try {
			errorbufferWriter.write(s);
			errorbufferWriter.newLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	  
      }
}
