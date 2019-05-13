import java.util.Map;
import javafx.util.Pair;
import java.util.ArrayList;
import java.util.LinkedHashMap; 
import java.util.LinkedList; 
import java.util.Queue;
import visitor.GJDepthFirst;
import syntaxtree.*;
@interface CaseOfMessageSendOnly{};

public class FirstVisitor extends GJDepthFirst<String, ClassData>{

    /* use a map list storing (class_name, meta_data) pairs */
    protected Map <String, ClassData> classes;
    protected Map <String, String> vars;
    protected LinkedList<String> messageQueue;
    private Integer nextVar, nextMethod;
    private String className;

    /* Constructor: initialize the map and the offsets*/ 
    public FirstVisitor(){
        this.classes = new LinkedHashMap<String, ClassData>();
        this.messageQueue = new LinkedList<String>();
        this.vars = new LinkedHashMap<String, String>();
        this.nextVar = new Integer(ClassData.pointerSize);
        this.nextMethod = new Integer(0);
    }

    /*  Goal
     f0 -> MainClass()
     f1 -> ( TypeDeclaration() )*
     f2 -> <EOF>
    */
    public String visit(Goal node, ClassData data){

        // visit main class in order to collect information about messages 
        node.f0.accept(this, null);

        // visit all user-defined classes 
        for(int i = 0; i < node.f1.size(); i++)
            node.f1.elementAt(i).accept(this, null);
        return null; 
    }

    @CaseOfMessageSendOnly
    /*  MainClass
        class f1 -> Identifier(){
            public static void main(String[] f11 -> Identifier()){ 
                f14 -> ( VarDeclaration() )*
                f15 -> ( Statement() )* 
        } */
    public String visit(MainClass node){
        this.className = node.f1.accept(this, null);
        ClassData cd = new ClassData(null);
        MethodData md = new MethodData(this.className, "void", 0, null);

   		for (int i = 0; i < node.f14.size(); i++)
 		   node.f14.elementAt(i).accept(this, cd);

        cd.methods.put("main", md);
        this.classes.put(this.className, cd);
   		return null;
    }

    /* TypeDeclaration
    f0 -> ClassDeclaration() | ClassExtendsDeclaration() */
    public String visit(TypeDeclaration node, ClassData data){

        /* initialize offsets for each new class*/
        this.nextVar = 0 + ClassData.pointerSize;
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
    public String visit(ClassDeclaration node, ClassData data){
        String id = node.f1.accept(this, null); 
        ClassData cd = new ClassData(null);
        this.className = id;

        /* pass ClassData to each field */
        for(int i = 0; i < node.f3.size(); i++)
            node.f3.elementAt(i).accept(this, cd);

        /* pass ClassData member method */
        for(int i = 0; i < node.f4.size(); i++)
            node.f4.elementAt(i).accept(this, cd);
        
        cd.setSize();
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
        String id = node.f1.accept(this, null), parent = node.f3.accept(this, null);
        this.className = id;

        /* Pass a meta data object down to the declarations sections, derived class inherits all fields and methods */ 
        ClassData cd = new ClassData(parent), cdParent = this.classes.get(parent);
        cd.vars.putAll(cdParent.vars);
        cd.methods.putAll(cdParent.methods);
        this.nextVar = cdParent.getOffsetOfNextVar();
        this.nextMethod = cd.methods.size() * ClassData.pointerSize;

        /* pass ClassData to each field */
    	for (int i = 0; i < node.f5.size(); i++)
            node.f5.elementAt(i).accept(this, cd);
            
        /* pass ClassData to each member method */
    	for (int i = 0; i < node.f6.size(); i++)
            node.f6.elementAt(i).accept(this, cd);

        cd.setSize();
        this.classes.put(id, cd);
    	return null;
    }

    /*  VarDeclaration
        f0 -> Type()
        f1 -> Identifier()
    bind each variable name/id to a type*/
    public String visit(VarDeclaration node, ClassData data){
        String type = node.f0.accept(this, null);
        String id = node.f1.accept(this, null);

        this.vars.put(id, type);

        /* store the variable and calculate the exact memory address for the next one to be stored */
        Pair<String, Integer> pair = new Pair<String, Integer>(type, this.nextVar);

        /* if it is not about a variable declared in a method, but in a class, update lookup Table */
        if(data != null){
            data.vars.put(id, pair);
            this.nextVar += ClassData.getSize(type).getKey();
        }
        return null;
    }

    /*  MethodDeclaration
        public f1 -> Type() f2 -> Identifier() (f4 -> ( FormalParameterList() )?){
            f7 -> ( VarDeclaration() )*
            f8 -> ( Statement() )*
            return f10 -> Expression();
        }
     */
    public String visit(MethodDeclaration node, ClassData data){
        String type = node.f1.accept(this, null);
        String id = node.f2.accept(this, null);

        /* get argument types, if they exist */
        ArrayList<Pair<String, String>> args = null;
    	if (node.f4.present())
            args = MyUtils.getParams(node.f4.accept(this, null).split(","));

        /* store method, if it already exists, it will be overriden*/
        MethodData method = new MethodData(this.className, type, this.nextMethod, args);
        data.methods.put(id, method);
        this.nextMethod += 8;
        return null;
    }

    /* FormalParameterList: f0 -> FormalParameter() f1 -> FormalParameterTail() Get all parameter types a String*/
    public String visit(FormalParameterList node, ClassData data){
    	String head = node.f0.accept(this, null), tail = node.f1.accept(this, null);
    	return head + tail;
    }

    /* FormalParameter f0 -> Type() f1 -> Identifier() Returns just the parameter type as a String*/
    public String visit(FormalParameter node, ClassData data){
        String type = node.f0.accept(this, null), id = node.f1.accept(this, null);
        this.vars.put(id, type);
        return type + ":" + id;
    }

    /* FormalParameterTail f0 -> ( FormalParameterTerm)* */
    public String visit(FormalParameterTail node, ClassData data){
        String retval = "";
        for (int i = 0; i < node.f0.size(); i++)
            retval += node.f0.elementAt(i).accept(this, null);
        return retval;
    }

    /* FormalParameterTerm: ,f1 -> FormalParameter */
    public String visit(FormalParameterTerm node, ClassData data){
        return "," + node.f1.accept(this,null);
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

    @CaseOfMessageSendOnly
    /*  Assignment Statement:   f0 -> Identifier() = f2 -> Expression(); */
    public String visit(AssignmentStatement node, ClassData data){ 
        String left = node.f0.accept(this, null), right = node.f2.accept(this, null);

        /* in case of an assignment, a parent class reference might now point to a child class object, 
           so adjust the variables table to reflect that, in order for the right method to be executed in case of a message send*/ 
        if(right != null)
            this.vars.put(left, right);
        return null;
    }

    @CaseOfMessageSendOnly
    /*MessageSend
    * f0 -> PrimaryExpression().f2 -> Identifier()(f4 -> ( ExpressionList() )?) */
    public String visit(MessageSend node, ClassData data){
        System.out.println("message: " + node.f2.accept(this, null));
        this.messageQueue.addLast(node.f0.accept(this, null));
        return null;
    }

    @CaseOfMessageSendOnly
    /*PrimaryExpression
    * f0 -> IntegerLiteral() | TrueLiteral() | FalseLiteral() | Identifier() 
    | ThisExpression() | ArrayAllocationExpression() | AllocationExpression() | BracketExpression() */
    public String visit(PrimaryExpression node, ClassData data){
        int childNode = node.f0.which;

        // in case of an identifier, return the corresponding data type
        if(childNode == 3)
            return this.vars.get(node.f0.accept(this, null));
        // in case of a 'this' pointer, return the name of this class
        else if(childNode == 4){
            System.out.println("this--> " + this.className);
            return this.className;
        }
        // in case of an allocation or a bracket expression, actual data type will be passed up 
        else if(childNode > 5)
            return node.f0.accept(this, null);
        else 
            return null;
    }

    @CaseOfMessageSendOnly
    /*AllocationExpresion:  new f1 -> Identifier()() */
    public String visit(AllocationExpression node, ClassData data){
        return node.f1.accept(this, null);
    }

    @CaseOfMessageSendOnly
    /*BracketExpression:    ( f1 -> Expression() )*/
    public String visit(BracketExpression node, ClassData data){
        return node.f1.accept(this, null);
    }
}