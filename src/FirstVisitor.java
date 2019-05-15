import java.util.*;
import javafx.util.Pair;
import visitor.GJDepthFirst;
import syntaxtree.*;
@interface CaseOfMessageSendOnly{};
@interface CaseOfWhileLoopOnly{};

public class FirstVisitor extends GJDepthFirst<String, ClassData>{

    /* use a map list storing (class_name, meta_data) pairs */
    protected Map <String, ClassData> classes;
    protected Map <String, String> vars;
    protected LinkedList<String> messageQueue;         // hold object type of each MessageSend
    protected LinkedList<Set<String>> loopsQueue; // for each loop, hold all loop-modified variables
    protected Set<String> loopTable;
    private Integer nextVar, nextMethod;
    private String className;

    /* Constructor: initialize the map and the offsets*/ 
    public FirstVisitor(){
        this.classes = new LinkedHashMap<String, ClassData>();
        this.messageQueue = new LinkedList<String>();
        this.loopsQueue = new LinkedList<Set<String>>();
        this.loopTable = null;
        this.vars = new LinkedHashMap<String, String>();
        this.nextVar = new Integer(ClassData.pointerSize);
        this.nextMethod = new Integer(0);
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
        node.f14.accept(this, cd);
        node.f15.accept(this, null); // info about MessageSend need to be collected on this pass

        cd.methods.put("main", md);
        this.classes.put(this.className, cd);
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

        /* initialize offsets */
        this.nextVar = 0 + ClassData.pointerSize;
        this.nextMethod = 0;

        /* pass ClassData to each field */
        node.f3.accept(this, cd);

        /* pass ClassData to each member method */
        node.f4.accept(this, cd);
 
        /* enter class data collected in the symbol table */
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
        this.nextMethod = cd.methods.size();

        /* pass ClassData to each field */
        node.f5.accept(this, cd);
            
        /* pass ClassData to each member method */
        node.f6.accept(this, cd);

        /* enter class data collected in the symbol table */        
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

        /* visit both all statement nodes and the return statement expression in order to detect any messages send*/
        node.f8.accept(this, null);
        node.f10.accept(this, null);

        /* if method already exists, override it by defining this class as the last to implement it
           other fields like return type or arguments do not need to be update it, mini-java does not support parametric polymorphism*/
        if(data.methods.containsKey(id))
            data.methods.put(id, new MethodData(this.className, type, data.methods.get(id).offset, args));
        else
            data.methods.put(id, new MethodData(this.className, type, this.nextMethod++, args));
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
        int which = node.f0.which;
        if (which == 0)
            return "array";
        else if(which == 1)
            return "boolean"; 
        else if(which == 2)
            return "integer"; 
        else
            return node.f0.accept(this, null);  // identifier
    }
    
    /* Identifier f0: return the id as a string*/
    public String visit(Identifier node, ClassData data){
        return node.f0.toString();
    }

    @CaseOfWhileLoopOnly
    /*WhileStatement
    * while( f2 -> Expression()) f4 -> Statement() */
    public String visit(WhileStatement node, ClassData data){

        // create loop table for AssignmentExpression node
        this.loopTable = new HashSet<String>();
        node.f2.accept(this, null);
        node.f4.accept(this, null);

        // push result to FIFO loop-queue
        this.loopsQueue.addLast(this.loopTable);

        // set loopTable to null for future assignments, until the next loop
        this.loopTable = null;
        return null;
    }


    @CaseOfWhileLoopOnly
    @CaseOfMessageSendOnly
    /*  AssignmentStatement:   f0 -> Identifier() = f2 -> Expression(); */
    public String visit(AssignmentStatement node, ClassData data){ 
        String left = node.f0.accept(this, null), right = node.f2.accept(this, null);

        /* in case of an assignment, a parent class reference might now point to a child class object, 
           so adjust the variables table to reflect that, in order for the right method to be executed in case of a message send*/ 
        if(right != null)
            this.vars.put(left, right);

        /* in case we are in a loop mark the identifier on the left as modified */
        if(this.loopTable != null)
            loopTable.add(left);  
        return null;
    }

    @CaseOfMessageSendOnly
    /*MessageSend
    * f0 -> PrimaryExpression().f2 -> Identifier()(f4 -> ( ExpressionList() )?) */
    public String visit(MessageSend node, ClassData data){
        String className = node.f0.accept(this, null);
        this.messageQueue.addLast(className);
        node.f4.accept(this, null); // visit ExpressionList to record MessageSends in there as well
        return className;           // return className because this node might be an inner/nested MessageSend
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
        // in case of a 'this' pointer, the name of this class is the appropriate data type
        else if(childNode == 4)
            return this.className;
        // in case of an allocation or a bracket expression, actual data type will be passed up 
        else if(childNode > 5)
            return node.f0.accept(this, null);
        else 
            return null;
    }

    @CaseOfMessageSendOnly
    /*AllocationExpresion:  new f1 -> Identifier()() */
    public String visit(AllocationExpression node, ClassData data){
        return node.f1.accept(this, null);  //return class name of new instance
    }

    @CaseOfMessageSendOnly
    /*BracketExpression:    ( f1 -> Expression() )*/
    public String visit(BracketExpression node, ClassData data){
        return node.f1.accept(this, null);
    }
}