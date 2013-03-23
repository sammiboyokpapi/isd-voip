package sip4me.gov.nist.core;
import java.io.PrintStream;


public class Debug {
    
  /**
   * Change this parameter to true to kick in the debug printfs
   * Need to make this final to save heap space.
   */
    public static boolean parserDebug = false;
    public static boolean debug = false;
    public static boolean LOG_FILE=false;
    private static  PrintStream trace = System.out;
    private static String debugFile="nist-log.txt";
    public static boolean systemOutput=false;
    
    
    public static void enableDebug(boolean deb) {
        debug = deb;
        systemOutput = deb;
    }
   
    
    public static void printStackTrace(String where,Exception ex) {
       if (debug) {
            trace.print(where+":");
            ex.printStackTrace();
            trace.println();
       }
    }
    
 /*
    public static void writeFile(String inFile,String outFile, String text, boolean sep) {
        // we read this file to obtain the options
        try{
            FileWriter fileWriter = new FileWriter(outFile,true);
            PrintWriter pw = new PrintWriter(fileWriter,false);
            
            if (text==null) {
                pw.println();
            }
            else
            if (sep) {
                 pw.print(text);
            }
            else {
                 pw.println(text);
            }
           
            pw.close();
            fileWriter.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeFile(String outFile, String text,boolean flush) {
        // we read this file to obtain the options
        try{
            FileWriter fileWriter = new FileWriter(outFile,false);
            PrintWriter pw = new PrintWriter(fileWriter,flush);
            
            if (text==null) {
                pw.println();
            }
            else
            
            pw.println(text);
           
            pw.close();
            fileWriter.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
  */
    
    public static void println(String text){
        if (systemOutput) 
            System.out.println(text);
        //else
       //     writeFile(debugFile,debugFile,text,false);
    }

    public static void println(){
        if (systemOutput) 
            System.out.println();
      //  else
       //     writeFile(debugFile,debugFile,null,false);
    }
    
    public static void print(String text){
        if (systemOutput) 
            System.out.print(text);
        //else
        //    writeFile(debugFile,debugFile,text,true);
    }
    
}
