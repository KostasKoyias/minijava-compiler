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

    Generatellvm(BufferedWriter out, Map<String, ClassData> data){
        this.out = out;
        this.data = data;
    }  

	private void emit(String s){
		try{
            this.out.write(s + "\n");
        }
		catch(IOException ex){
			System.err.println(ex.getMessage());
		}
    }

    private String declareMethods(Map<String, Pair<String, Integer>> methods){
        String ret = "";
        for(Map.Entry<String, Pair<String, Integer>> entry : methods.entrySet())
            ret += "i8* bitcast (" + ClassData.offsets.get(entry.getValue().getKey())*8 + " (args)*" + " @" + entry.getKey() + " to i8*)";
        return ret;
    }

    private void declareVTable(String className, ClassData data){
        int methods = data.methods.size();
        emit("@." + className + "_vtable = global [" + methods + "x i8*] [" + this.declareMethods(data.methods) + "]");
        //@.Fac_vtable = global [1 x i8*] [i8* bitcast (i32 (i8*,i32)* @Fac.ComputeFac to i8*)]
    }
    
    /*  Goal
     f0 -> MainClass()
     f1 -> ( TypeDeclaration() )*
     f2 -> <EOF>
    */
    public String visit(Goal node){
        for(Map.Entry<String, ClassData> entry : this.data.entrySet())
            this.declareVTable(entry.getKey(), entry.getValue());

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
        // pass name of the class to child nodes 
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
        // pass name of the class to child nodes 
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