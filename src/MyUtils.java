/* utility functions extracted from Generatellvm.java during Refactoring in order to increase modularity and simplify code */

import java.io.*;
import javafx.util.Pair;
import java.util.ArrayList;
import java.util.LinkedHashMap; 
import java.util.*;

public abstract class MyUtils{
    
        // given an array of mini-java types, return a comma-separated String with their corresponding llvm types (e.g int -> i32)  
        protected static String getArgs(ArrayList<Pair<String, String>> args, boolean isDefinition){
            // first arg of any member method is always a pointer(8 bits) to the object its self, called "this"
            String rv = " (i8*" + (isDefinition ? " %this" : ""), type; 
            int i = 0;
            if(args != null){
                for(Pair<String, String> arg : args){
                    type = ClassData.getSize(arg.getKey()).getValue();
                    rv += (i++ < args.size() ? ", " : "") + type + (isDefinition ? " %." + arg.getValue() : ""); 
                }
            }
            return rv + ")" + (isDefinition ? "" : "*");
        }
    
        // return a comma-separated String representing all methods of a class in low-level
        protected static String declareMethods(String className, Map<String, MethodData> methods){
            String rv = "", methodName, retType;
            int i = 0;
    
            // for each method append: 1.return type and 2.arguments to the String to be returned
            for(Map.Entry<String, MethodData> entry : methods.entrySet()){
                retType = ClassData.getSize(entry.getValue().returnType).getValue();
                methodName = entry.getKey();
                rv += "i8* bitcast (" + retType + MyUtils.getArgs(entry.getValue().arguments, false) + " @" + entry.getValue().className + "." + methodName + " to i8*)";
                rv += ++i < methods.size() ? ", " : ""; 
            }
            return rv;
        }
    
        // declare a global vTable for a class
        protected static void declareVTable(Generatellvm obj){
            String className;
            Map<String, MethodData> methods;

            // for each class, declare a global vTable in the .ll file
	    obj.emit(";for each class, declare a global vTable containing a pointer for each method");
            for(Map.Entry<String, ClassData> entry : obj.data.entrySet()){
                className = entry.getKey();
                methods = entry.getValue().methods;
                obj.emit("@." + className + "_vtable = global [" + methods.size() + " x i8*] [" + MyUtils.declareMethods(className, methods) + "]");
            }
        }

        // return all parameters of a member-method in the appropriate llvm form
        protected static ArrayList<Pair<String, String>> getParams(String[] params){
            ArrayList<Pair<String, String>> rv = new ArrayList<Pair<String, String>>();
            int splitAt;
            for(String par : params){
                splitAt = par.indexOf(':');
                if(splitAt != -1)
                    rv.add(new Pair<String, String>(par.substring(0, splitAt), par.substring(splitAt+1)));
            }
            return rv;
        }

        // get argument list in of form (type_0 arg0, ..., type_n arg_n) and filter it, so that it gets the form of (type_0, ..., type_n)
        public static String filterSignature(String signature, String classPointer){
            String[] args = signature.split(",");
            String rv = "";
            int len = args.length;
            
            if(len == 1)
                return "(i8*)" ;
            for(int i = 0; i < len; i++)
                rv += args[i].trim().split(" ")[0] + (i + 1 ==  len ? ")" : ", ");
            return rv;
        }
}
