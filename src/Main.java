import syntaxtree.*;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;

import javafx.util.Pair;
import java.io.*;

class Main {
    public static void main (String [] args) throws Exception{
        FileInputStream fin = null;
        FileOutputStream fout = null;

        /* for each file path given from the cmd */
        for(String arg : args){

            /* try and: open, parse and visit the syntax tree of the program */
            try{

                fin = new FileInputStream(arg);
                fout = new FileOutputStream(arg.replace("java", "ll"));
                MiniJavaParser parser = new MiniJavaParser(fin);

                /* first, traverse the tree to get offset data for each class */
                FirstVisitor v0 = new FirstVisitor();
                Goal root = parser.Goal();
                root.accept(v0, null);

                System.out.println("Offsets\n-------");

                /* For each class */
                for (Map.Entry<String, ClassData> entry : v0.classes.entrySet()) {
                    String name = entry.getKey();
                    System.out.println("Class: " + name);

                    /* For each variable of the class, print offset */
                    System.out.println("\n\tFields\n\t------\n");
                    for(Map.Entry<String, Pair<String, Integer>> var : entry.getValue().vars.entrySet())
                        System.out.println("\t\t" + name + "." + var.getKey() + ": " + var.getValue().getValue());

                    /* For each pointer to a member method, print offset */
                    System.out.println("\n\tMethods\n\t-------\n");
                    for(Map.Entry<String, Pair<String, Integer>> func : entry.getValue().methods.entrySet())
                        System.out.println("\t\t" + name + "." + func.getKey() + ": " + func.getValue().getValue());                       
                }   
            }
            /* handle exceptions */
            catch(ParseException ex){
                System.out.println(ex.getMessage());
            }
            catch(FileNotFoundException ex){
                    System.err.println(ex.getMessage());
            }

            /* clean things up */
            finally{
                try{
                    if(fin != null) fin.close();
                    if(fout != null) fout.close();
                }
                catch(IOException ex){
                    System.err.println(ex.getMessage());
                }
            }

        }
    }
}