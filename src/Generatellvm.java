import java.io.*;
import syntaxtree.*;
import javafx.util.Pair;
import java.util.LinkedHashMap; 
import visitor.GJNoArguDepthFirst;
import java.util.*;

public class Generatellvm extends GJNoArguDepthFirst<String>{
    private BufferedWriter out;
    private Map<String, ClassData> data;
    private String className;

    // Constructor: set a pointer to output file and set class data collected during the first pass
    Generatellvm(BufferedWriter out, Map<String, ClassData> data){
        this.out = out;
        this.data = data;
    }  

    // append a String in the file to be generated
	private void emit(String s){
		try{
            this.out.write(s + "\n");
        }
		catch(IOException ex){
			System.err.println(ex.getMessage());
		}
    }

    // given an array of mini-java types, return a comma-separated String with their corresponding llvm types (e.g int -> i32)  
    private String getArgs(String[] args){
        String rv = "";
        int i = 0;
        if(args != null)
            for(String arg : args)
                rv += ClassData.sizes.get(arg).getValue() + (++i < args.length ? ", " : "");
        return rv;
    }

    // return a comma-separated String representing all methods of a class in low-level
    private String declareMethods(String className, Map<String, Triplet<String, Integer, String[]>> methods){
        String rv = "", methodName, retType;
        int i = 0;

        // for each method append: 1.return type and 2.arguments to the String to be returned
        for(Map.Entry<String, Triplet<String, Integer, String[]>> entry : methods.entrySet()){
            retType = ClassData.sizes.get(entry.getValue().getFirst()).getValue();
            methodName = entry.getKey();
            rv += "i8* bitcast (" + retType + " (" + this.getArgs(entry.getValue().getThird()) + ")* @" + className + "." + methodName + " to i8*)";
            rv += ++i < methods.size() ? ", " : ""; 
        }
        return rv;
    }

    // declare a global vTable for a class
    private void declareVTable(String className, ClassData data){
        int methods = data.methods.size();
        emit("@." + className + "_vtable = global [" + methods + "x i8*] [" + this.declareMethods(className, data.methods) + "]");
    }
    
    /*  Goal
     f0 -> MainClass()
     f1 -> ( TypeDeclaration() )*
     f2 -> <EOF>
    */
    public String visit(Goal node){

        // for each class, declare a global vTable in the .ll file
        for(Map.Entry<String, ClassData> entry : this.data.entrySet())
            this.declareVTable(entry.getKey(), entry.getValue());

        // define some utility functions in the .ll file
        emit("\n\n"
            + "declare i8* @calloc(i32, i32)\n"
            + "declare i32 @printf(i8*, ...)\n"
            + "declare void @exit(i32)\n\n"
            
            + "@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n"
            + "@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n"
            + "define void @print_int(i32 %i) {\n"
            +    "\t%_str = bitcast [4 x i8]* @_cint to i8*\n"
            +    "\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n"
            +    "\tret void\n}\n\n"
            
            
            + "define void @throw_oob() {\n"
            +    "\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n"
            +    "\tcall i32 (i8*, ...) @printf(i8* %_str)\n"
            +    "\tcall void @exit(i32 1)\n"
            +    "\tret void\n}\n");    
        node.f0.accept(this);


        // visit all user-defined classes 
        /*for(int i = 0; i < node.f1.size(); i++)
            node.f1.elementAt(i).accept(this);*/
        return null; 
    }

    /*  MainClass
        class f1 -> Identifier(){
            public static void main(String[] f11 -> Identifier()){ 
                f14 -> ( VarDeclaration() )*
                f15 -> ( Statement() )* 
        } 
    
    public String visit(MainClass node){
        this.className = node.f1.accept(this);
   		
   		for (int i = 0; i < node.f14.size(); i++)
 		   node.f14.elementAt(i).accept(this);

   		for (int i = 0; i < node.f15.size(); i++)
               node.f15.elementAt(i).accept(this);

   		return null;
}*/

   /* ClassDeclaration
    class f1 -> Identifier(){
        f3 -> ( VarDeclaration() )*
        f4 -> ( MethodDeclaration() )*
    }
    
    public String visit(ClassDeclaration node) throws RuntimeException {
        // pass methodName of the class to child nodes 
        this.className = node.f1.accept(this);

        for (int i = 0; i < node.f3.size(); i++)
            node.f3.elementAt(i).accept(this);

        for (int i = 0; i < node.f4.size(); i++)
            node.f4.elementAt(i).accept(this);
        return null;
    }*/

    /*
        class f1 -> Identifier() f2 -> "extends" f3 -> Identifier(){}
            f5 -> ( VarDeclaration() )*
            f6 -> ( MethodDeclaration() )*
        }
    
    public String visit(ClassExtendsDeclaration node) throws RuntimeException {
        // pass methodName of the class to child nodes 
        this.className = node.f1.accept(this);
       
        for (int i = 0; i < node.f5.size(); i++)
            node.f5.elementAt(i).accept(this);

        for (int i = 0; i < node.f6.size(); i++)
            node.f6.elementAt(i).accept(this);
        return null;
    }*/


    /*Identifier
    * f0 -> <IDENTIFIER>*/
    public String visit(Identifier node){
        return node.f0.toString();
    }


}