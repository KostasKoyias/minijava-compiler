import java.util.Map;
import javafx.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap; 
import visitor.GJDepthFirst;
import syntaxtree.*;

public class FirstVisitor extends GJDepthFirst<String, ClassData>{

    /* use a map list storing (class_name, meta_data) pairs */
    public Map <String, ClassData> classes;
    private Integer nextVar, nextMethod;

    /* map all mini java data types to their actual size in bytes */
    private final Map<String, Integer> offsets = new LinkedHashMap<String, Integer>() {
        private static final long serialVersionUID = 1L;
        {
            put("boolean", 1);
            put("integer", 4);
        }
    };
       

    /* Constructor: initialize the map */ 
    public FirstVisitor(){
        this.classes = new LinkedHashMap<String, ClassData>();
        this.nextVar = new Integer(0);
        this.nextMethod = new Integer(0);
    }

    /*  Goal
     f0 -> MainClass()
     f1 -> ( TypeDeclaration() )*
     f2 -> <EOF>
    */
    public String visit(Goal node, ClassData data) throws RuntimeException{

        // omit Main class, visit all user-defined classes 
        for(int i = 0; i < node.f1.size(); i++)
            node.f1.elementAt(i).accept(this, null);
        return null; 
    }

    /* TypeDeclaration
    f0 -> ClassDeclaration() | ClassExtendsDeclaration() */
    public String visit(TypeDeclaration node, ClassData data) throws RuntimeException{

        /* initialize offsets for each new class*/
        this.nextVar = 8;
        this.nextMethod = 0;
        node.f0.accept(this, null);
        return null;
    }

    /* ClassDeclaration
    class f1 -> Identifier(){
        f3 -> ( VarDeclaration() )*
        f4 -> ( MethodDeclaration() )*
    }
    */
    public String visit(ClassDeclaration node, ClassData data) throws RuntimeException{
        String id = node.f1.accept(this, null);
        ClassData cd = new ClassData(null);

        /* pass ClassData to each field */
        for(int i = 0; i < node.f3.size(); i++)
            node.f3.elementAt(i).accept(this, cd);

        /* pass ClassData member method */
        for(int i = 0; i < node.f4.size(); i++)
            node.f4.elementAt(i).accept(this, cd);
        
        this.classes.put(id, cd);
        return null;
    }

    /*
        class f1 -> Identifier() f2 -> "extends" f3 -> Identifier(){}
            f5 -> ( VarDeclaration() )*
            f6 -> ( MethodDeclaration() )*
        }
    */
    public String visit(ClassExtendsDeclaration node, ClassData data){
    	String id = node.f1.accept(this, null);
    	String parent = node.f3.accept(this, null);

        /* Pass a meta data object down to the declarations sections, derived class inherits all fields and methods */ 
        ClassData cd = new ClassData(parent), cdParent = this.classes.get(parent);
        cd.vars.putAll(cdParent.vars);
        //cd.methods.putAll(cdParent.methods);

        /* pass ClassData to each field */
    	for (int i = 0; i < node.f5.size(); i++)
            node.f5.elementAt(i).accept(this, cd);
            
        /* pass ClassData to each member method */
    	for (int i = 0; i < node.f6.size(); i++)
            node.f6.elementAt(i).accept(this, cd);

        this.classes.put(id, cd);
    	return null;
    }

    /*  VarDeclaration
        f0 -> Type()
        f1 -> Identifier()
    bind each variable name/id to a type*/
    public String visit(VarDeclaration node, ClassData data) throws RuntimeException{
        String type = node.f0.accept(this, null);
        String id = node.f1.accept(this, null);

        /* store the variable and calculate the exact memory address for the next one to be stored */
        Pair<String, Integer> pair = new Pair<String, Integer>(type, this.nextVar);

        /* if it is not about a variable declared in a method, but in a class, update lookup Table */
        if(data != null){
            this.nextVar += this.offsets.containsKey(type) ? this.offsets.get(type) : 8;
            data.vars.put(id, pair);
        }
        return null;
    }

    /*
        public f1 -> Type() f2 -> Identifier() (f4 -> ( FormalParameterList() )?){
            f7 -> ( VarDeclaration() )*
            f8 -> ( Statement() )*
            return f10 -> Expression();
        }
     */
    public String visit(MethodDeclaration node, ClassData data){
        String type = node.f1.accept(this, null);
        String id = node.f2.accept(this, null);
    	
    	/* check whether the method overrides a super class method */	
        String parent = data.parentName;
        ClassData parentClassData = this.classes.get(parent);
        boolean over = parentClassData != null && parentClassData.methods.containsKey(id);

        /* store a pointer to the method and calculate the exact memory address for the next one to be stored */
        Pair<String, Integer> pair = new Pair<String, Integer>(type, this.nextMethod);
        if(!over){
            this.nextMethod += 8;
            data.methods.put(id, pair);
        }
        return null;
    }

    /* Type: f0 -> ArrayType() | BooleanType() | IntegerType() | Identifier() */
    public String visit(Type node, ClassData data){
        return node.f0.accept(this, null);
    }

    /* Return each primitive type of MiniJava(int, int [] and boolean) as a String */ 
    public String visit(ArrayType node, ClassData data){
        return "array";
    }

    public String visit(BooleanType node, ClassData data){
        return "boolean";
    }

    public String visit(IntegerType node, ClassData data){
        return "integer";
    }
    
    /* Identifier f0: return the id as a string*/
    public String visit(Identifier node, ClassData data){
        return node.f0.toString();
    }
}