import syntaxtree.*;
import java.util.*;
import javafx.util.Pair;
import java.io.*;

class Main {
    public static void main (String [] args) throws Exception{
        FileInputStream fin = null;
        BufferedWriter fout = null;
        boolean displayOffsets = Arrays.asList(args).contains("--offsets");

        /* for each file path given from the cmd */
        for(String arg : args){
            if(arg.equals("--offsets"))
                continue;

            /* try and: open, parse and visit the syntax tree of the program */
            try{

                fin = new FileInputStream(arg);
                MiniJavaParser parser = new MiniJavaParser(fin);

                /* first, traverse the tree to get offset data for each class */
                FirstVisitor v0 = new FirstVisitor();
                Goal root = parser.Goal();
                root.accept(v0, null);
                
                if(displayOffsets){
                    System.out.println("Offsets\n-------");

                    /* For each class */
                    for (Map.Entry<String, ClassData> entry : v0.classes.entrySet()) {
                        String name = entry.getKey();
                        System.out.println("Class: " + name);

                        /* For each variable of the class, print offset */
                        System.out.println("\n\tFields\n\t------\n\t\tthis: 0");
                        for(Map.Entry<String, Pair<String, Integer>> var : entry.getValue().vars.entrySet())
                            System.out.println("\t\t" + name + "." + var.getKey() + ": " + var.getValue().getValue());

                        /* For each pointer to a member method, print offset */
                        System.out.println("\n\tMethods\n\t-------");
                        for(Map.Entry<String, MethodData> func : entry.getValue().methods.entrySet())
                            System.out.println("\t\t" + func.getValue().className + "." + func.getKey() + ": " + func.getValue().offset);                       
                    }  
                    
                }

                /* generate intermediate representation code */
                fout = new BufferedWriter(new FileWriter(arg.replace(".java", ".ll")));
                Generatellvm v1 = new Generatellvm(fout, v0.classes, v0.messageQueue, v0.loopsQueue);
                root.accept(v1);
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
        if(!displayOffsets)
            System.out.println("To view field and method offsets for each class rerun with --offsets");
    }
}