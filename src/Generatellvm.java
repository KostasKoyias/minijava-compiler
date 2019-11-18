import java.io.*;
import syntaxtree.*;
import javafx.util.Pair;
import java.util.*; 
import visitor.GJNoArguDepthFirst;

public class Generatellvm extends GJNoArguDepthFirst<String>{
    private BufferedWriter out;
    protected Map<String, ClassData> data;
    private LinkedList<String> messageQueue;
    private String className;
    private State state; 
    private boolean inIfStatement;

    // Constructor: set a pointer to output file and set class data collected during the first pass
    Generatellvm(BufferedWriter out, Map<String, ClassData> data, LinkedList<String> messageQueue){
        this.out = out;
        this.data = data;
        this.state = new State();
        this.messageQueue = messageQueue;  
    }

    // given a register or an integer add 1 to it because 1st place of an array is reserved for the length to be stored at, return the result
    private String getArrayIndex(String index){

        // if a register was passed, add 1 to it and store the result in a register who will be returned
        if(index.startsWith("%")){
            emit("\n\t" + this.state.newReg() + " = add i32 " + index + ", 1");
            return "%_" + (this.state.getRegCounter()-1);
        }

        // else index is a number, just add 1 to it and return the result
        return String.valueOf(Integer.parseInt(index) + 1);
    }

    // given a field of a class, return a register holding the address of that field
    // fields need to always get re-loaded because of while loops and if statements where we need the most recent value in each case possible
    private String getField(String field, boolean wantContent){
        Pair<String, Integer> fieldInfo = this.data.get(this.className).vars.get(field);
        String reg = this.state.newReg(), llvmType = ClassData.getSize(fieldInfo.getKey()).getValue(); 

        // set a pointer to the field
        emit("\n\t;load " + (wantContent ? "field " : "address of ") + this.className + "." + field + " from memory" 
            + "\n\t" + reg + " = getelementptr i8, i8* %this, i32 " + fieldInfo.getValue());

        // cast field pointer to actual size of field if it it is different than i8
        if(!"i8".equals(llvmType))
            emit("\t" + this.state.newReg() + " = bitcast i8* " + reg + " to " + llvmType + "*");

        // if the actual content of the field was requested, load it to a register that will be returned 
        if(wantContent)
            emit("\t" + this.state.newReg() + " = load " + llvmType + ", " + llvmType + "* " + ("%_" + (this.state.getRegCounter()-2)));
        return llvmType + (wantContent ? " " : "* ")  + "%_" + (this.state.getRegCounter()-1);
    }

    // given an identifier return a pair containing the register that holds the address of the id, and the type of the id
    private String getIdAddress(String id){
        State.IdInfo info = this.state.getIdInfo(id);

        // if identifier is a field of this class, load the address of that field on a brand new register
        // else there is already a register holding the address of the identifier, either way return a register holding the address of the id 
        return (info == null) ? this.getField(id, false) : info.type + "* " + info.register;
    }

    // append a String in the file to be generated
	protected void emit(String s){
		try{
            this.out.write(s + "\n");
        }
		catch(IOException ex){
			System.err.println(ex.getMessage());
		}
    }
    
    /*  Goal
     f0 -> MainClass()
     f1 -> ( TypeDeclaration() )*
     f2 -> <EOF>
    */
    public String visit(Goal node){

        // for each class, declare a global vTable in the .ll file
        //for(Map.Entry<String, ClassData> entry : this.data.entrySet())
            //this.declareVTable(entry.getKey(), entry.getValue());
        MyUtils.declareVTable(this);

        // define some utility functions in the .ll file
        emit("\n"
            + ";declare functions to be used\n"
            + "declare i8* @calloc(i32, i32)\n"
            + "declare i32 @printf(i8*, ...)\n"
            + "declare void @exit(i32)\n\n"
            
            + ";define constants and functions to be used\n"
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
            +    "\tret void\n}\n\n"
            + "@_ctrue = constant [6 x i8] c\"true\\0a\\00\"\n"
            + "@_cfalse = constant [7 x i8] c\"false\\0a\\00\"\n"

            + "define void @print_bool(i1 %i){\n"
            +  "\tbr i1 %i, label %is_true, label %is_false\n\n"
            + "is_true:\n"
            +   "\t%_res_true = bitcast [6 x i8]* @_ctrue to i8*\n"
            +   "\tbr label %result\n\n"
            + "is_false:\n"
            +   "\t%_res_false = bitcast [7 x i8]* @_cfalse to i8*\n"
            +   "\tbr label %result\n\n"
            + "result:\n"
            +   "\t%_res = phi i8* [%_res_true, %is_true], [%_res_false, %is_false]\n" 
            +   "\tcall i32 (i8*, ...) @printf(i8* %_res)\n"
            +   "\tret void\n}\n");
        
        // visit main
        node.f0.accept(this);

        // visit all user-defined classes 
        node.f1.accept(this);
        return null; 
    }

    /*  MainClass
        class f1 -> Identifier(){
            public static void main(String[] f11 -> Identifier()){ 
                f14 -> ( VarDeclaration() )*
                f15 -> ( Statement() )* 
        } 
    */
    public String visit(MainClass node){
        this.className = node.f1.accept(this);
        emit("define i32 @main() {");

        // visit variable declarations and statements
 		node.f14.accept(this);
        node.f15.accept(this);
        
        emit("\tret i32 0\n}\n");
   		return null;
    }

    /*Type Declaration
      f0 -> ClassDeclaration()    |   ClassExtendsDeclaration() */
    public String visit(TypeDeclaration node){
        node.f0.accept(this);
        return null;
    }

   /* ClassDeclaration
    class f1 -> Identifier(){
        f3 -> ( VarDeclaration() )*
        f4 -> ( MethodDeclaration() )*
    }*/
    public String visit(ClassDeclaration node){

        // set class name for children to know 
        this.className = node.f1.accept(this);

        // visit all member methods
        node.f4.accept(this);
        return null;
    }

    /*  ClassExtendsDeclaration
        class f1 -> Identifier() f2 -> "extends" f3 -> Identifier(){}
            f5 -> ( VarDeclaration() )*
            f6 -> ( MethodDeclaration() )*
        }
    */
    public String visit(ClassExtendsDeclaration node){

        // set class name for children to know 
        this.className = node.f1.accept(this);

        // visit all member methods
        node.f6.accept(this);
        return null;
    }

    /*VarDeclaration
    * f0 -> Type()
    * f1 -> Identifier()
    */
    public String visit(VarDeclaration node){
    
        // allocate space and store local variable
        String varType = ClassData.getSize(node.f0.accept(this)).getValue(), id = node.f1.accept(this);
        emit("\n\t;allocate space for local variable %" + id + "\n\t%" + id + " = alloca " + varType);
        this.state.put(id, "%" + id, varType); // keep track of the register holding that address
        return null;
    }

    /*  MethodDeclaration
        public f1 -> Type() f2 -> Identifier() (f4 -> ( FormalParameterList() )?){
            f7 -> ( VarDeclaration() )*
            f8 -> ( Statement() )*
            return f10 -> Expression();
        }
    */
    public String visit(MethodDeclaration node){
       
        // get return type of method in the appropriate llvm form
        String id = node.f2.accept(this), returnType = ClassData.getSize(node.f1.accept(this)).getValue();

        // emit method's signature
        ArrayList<Pair<String, String>> parameters = this.data.get(this.className).methods.get(id).arguments;
        emit(";" + this.className + "." + id  + "\ndefine " + returnType + " @" + this.className + "." + id + MyUtils.getArgs(parameters, true) + "{");	  
        
        // allocate space and store each parameter of the method, associate each parameter with a register holding the parameter's address 
        String llvmType, paramID;
        if(parameters != null){
            emit("\t;allocate space and store each parameter of the method");
            for(Pair<String, String> par : parameters){
                paramID = par.getValue();
                llvmType = ClassData.getSize(par.getKey()).getValue();
                emit("\t%" + paramID + " = alloca " + llvmType +
                     "\n\tstore " + llvmType + " %." + paramID + ", " + llvmType + "* %" + paramID);
                this.state.put(paramID, "%" + paramID, llvmType);
            } 
        }  

        // visit variable declarations
        node.f7.accept(this);
        
        // visit statements 
        node.f8.accept(this);

        emit("\tret " + node.f10.accept(this) + "\n}\n");  
        this.state.clear();
        return null;
    }

    /* Statement: f0 -> Block() | AssignmentStatement() | ArrayAssignmentStatement() | IfStatement() | WhileStatement() | PrintStatement() */
    public String visit(Statement node){
        node.f0.accept(this);
        return null;
    }

    /*  Block: {( Statement() )*} */
    public String visit(Block node){
        node.f1.accept(this);
        return null;
    }

    /*  Assignment Statement:   f0 -> Identifier() = f2 -> Expression(); */
    public String visit(AssignmentStatement node){ 
        String leftID = node.f0.accept(this), rightSide = node.f2.accept(this), leftType, leftReg, rightType = rightSide.split(" ")[0];
        String[] leftInfo = this.getIdAddress(leftID).split(" "); leftType = leftInfo[0]; leftReg = leftInfo[1];  // get id's type and register

        // if types do not match, cast left operand to the appropriate pointer type
        if(leftType.equals(rightType + "*") == false){
            emit("\n\t;adjust pointer type of left operand\n\t" 
                 + this.state.newReg() + " = bitcast " + leftType + " " + leftReg + " to " + rightType + "*");
            leftType = rightType + "*";            
            leftReg = "%_" + (this.state.getRegCounter()-1);
        }

        // store the content of the address calculated for the right side, at the address calculated for the left side
        emit("\n\t;store result\n\tstore " + rightSide + ", " + leftType + " " + leftReg);
        return null;
    }

    /*  ArrayAssignmentStatement:   f0 -> Identifier() [f2 -> Expression()] = f5 -> Expression(); */
    public String visit(ArrayAssignmentStatement node){
        String leftID = node.f0.accept(this), leftInfo = this.getIdAddress(leftID), index = node.f2.accept(this).split(" ")[1], rightSide = node.f5.accept(this);
        
        // make sure array index is within bounds
        this.checkArrayIndex(leftInfo.split(" ")[1], index, false);   
        index = this.getArrayIndex(index);     

        // load a pointer to the array, cast it to integer pointer, get it to point at the index-th element and modify it
        emit("\n\t;assign a value to the array element\n\t" + this.state.newReg() + " = load i8*, " + leftInfo + "\n\t"
            + this.state.newReg() + " = bitcast i8* %_" + (this.state.getRegCounter()-2) + " to i32*\n\t"
            + this.state.newReg() + " = getelementptr i32, i32* %_" + (this.state.getRegCounter()-2) + " , i32 " + index
            +"\n\tstore " + rightSide + ", i32* %_" + (this.state.getRegCounter()-1));
        return null;
    }

    /*IfStatement
    * if( f2 -> Expression())
    *       f4 -> Statement()
    * else
    *       f6 -> Statement()
    */
    public String visit(IfStatement node){

        // get a set of if labels and load the if condition to a register
        String[] ifLabel = this.state.newLabel("if");
        String condition = node.f2.accept(this), brEnd = "\tbr label %" + ifLabel[2] + "\n\n";

        this.inIfStatement = true;
        emit("\n\t;if statement\n\tbr " + condition + " ,label %" + ifLabel[0] + ", label %" + ifLabel[1] +"\n\n" + ifLabel[0] + ":");
        node.f4.accept(this);
        emit(brEnd + ifLabel[1] + ":");
        node.f6.accept(this);
        emit(brEnd + ifLabel[2] + ":");
        this.inIfStatement = false;

        return null;
    }

    /*WhileStatement
    * while( f2 -> Expression()) f4 -> Statement() */
    public String visit(WhileStatement node){
        
        // get a set of while labels
        String[] whileLabel = this.state.newLabel("while");
        String condition;

        emit("\n\t;while statement\n\tbr label %" + whileLabel[0] + "\n\n" + whileLabel[0] + ":");
        condition = node.f2.accept(this); 
        emit("\tbr " + condition + " ,label %" + whileLabel[1] + ", label %" + whileLabel[2] + "\n\n" + whileLabel[1] + ":");
        node.f4.accept(this);
        emit("\n\tbr label %" + whileLabel[0] + "\n" + whileLabel[2] + ":\n");

        return condition;
    }

    /*Print Statement: System.out.println( f2 -> Expression());*/
	public String visit(PrintStatement node){
        String expr = node.f2.accept(this), type = expr.split(" ")[0];

		emit("\n\t;display an " + type + "\n\tcall void (" + type + ") @print_" + (type.equals("i1") ? "bool" : "int") + "(" + expr +")");
		return null;
	}

    /*Expression
    * f0 -> AndExpression() | CompareExpression() | PlusExpression() | MinusExpression() 
        | TimesExpression() | ArrayLookup() | ArrayLength() | MessageSend() | Clause()
    */
    public String visit(Expression node){
        return node.f0.accept(this);
    }

    /*MessageSend
    * f0 -> PrimaryExpression().f2 -> Identifier()(f4 -> ( ExpressionList() )?) */
    public String visit(MessageSend node){

        // get class name and address, as well as the method name 
        String classPointer = node.f0.accept(this), methodName = node.f2.accept(this), signature, returnType;
        MethodData methodData = this.data.get(this.messageQueue.removeFirst()).methods.get(methodName);

        // get offset and return type of method, also filter the signature getting just the type of it, not the exact arguments
        int offset = methodData.offset;
        returnType = ClassData.getSize(methodData.returnType).getValue();
        signature = node.f4.present() ? node.f4.accept(this).replaceFirst("[(]", "(" + classPointer + ", ") : "(" + classPointer + ")";


        emit("\t" + this.state.newReg() + " = bitcast " + classPointer + " to i8*** \t\t\t\t;%_" + (this.state.getRegCounter()-1) + " points to the vTable"
            +"\n\t" + this.state.newReg() + " = load i8**, i8*** %_" + (this.state.getRegCounter()-2) + "\t\t\t\t;%_"+ (this.state.getRegCounter()-1) 
            + " is the vTable\n\t" + this.state.newReg() + " = getelementptr i8*, i8** %_" + (this.state.getRegCounter()-2) + ", i32 " + offset + "\t;%_"
            +(this.state.getRegCounter()-1) + " points to the address of " + methodName
            +"\n\t" + this.state.newReg() + " = load i8*, i8** %_" + (this.state.getRegCounter()-2)
            + "\t\t\t\t\t;%_" + (this.state.getRegCounter()-1) + " points to the body of " + methodName
            +"\n\t" + this.state.newReg() + " = bitcast i8* %_" + (this.state.getRegCounter()-2) + " to " + returnType + " " 
            + MyUtils.filterSignature(signature, classPointer) + "*\t;%_cast pointer to the appropriate size\n\t" 
            + this.state.newReg() + " = call " + returnType + " %_" +(this.state.getRegCounter()-2) + signature);
        return returnType + " %_" + (this.state.getRegCounter()-1);	   
    }

    /*ExpressionList: f0 -> Expression() f1 -> ExpressionTail()*/
    public String visit(ExpressionList node){
        return "(" + node.f0.accept(this) + node.f1.accept(this) + ")";
    }

    /*ExpressionTail: f0 -> ( ExpressionTerm() )* */																		
    public String visit(ExpressionTail node){
        String rv = "";

        // collect all arguments
        for (int i = 0; i < node.f0.size(); i++)
            rv += node.f0.elementAt(i).accept(this);
        return rv;
    }

    /*ExpressionTerm: ,f1 -> Expression() */
    public String visit(ExpressionTerm node){
        return ", " + node.f1.accept(this);
    }


    /* given an index and a register holding the address of the array, load the array element requested in a register, length is stored at index 0 */
    public String getArrayElement(String id, String index){
        String comment = index.equals("0") ? ";get length of array at " + id.split(" ")[1] : ";lookup *(" + id.split(" ")[1] + " + " + index + ")";

        emit("\n\t" + comment + "\n"  
            +"\t" + this.state.newReg() + " = bitcast " + id + " to i32*\n"
            +"\t" + this.state.newReg() + " = getelementptr i32, i32* %_" + (this.state.getRegCounter()-2) + ", i32 " + index
            +"\n\t" + this.state.newReg() + " = load i32, i32* %_" + (this.state.getRegCounter()-2));    
        return "i32 %_" + (this.state.getRegCounter()-1);
    }

    /* given an array and an index, throw outOfBounds exception if index given is either negative or too large*/
    public void checkArrayIndex(String id, String index, boolean loaded){   
        String len;
        String[] label = this.state.newLabel("oob");

        // get length of array
        if(!loaded){
            emit("\n\t;load array\n\t" + this.state.newReg() + " = load i8*, i8** " + id);
            len = this.getArrayElement("i8* %_" + (this.state.getRegCounter()-1), "0").split(" ")[1];
        }
        else 
            len = this.getArrayElement(id, "0").split(" ")[1];

        // (index < 0) xor (index < array.length) ? then inBounds : else throw outOfBounds exception
        emit("\n\t;make sure index \"" + index + "\" is within bounds\n\t" + this.state.newReg() + " = icmp slt i32 " + index + ", 0"
            +"\n\t" + this.state.newReg() + " = icmp slt i32 " + index + ", " + len
            +"\n\t" + this.state.newReg() + " = xor i1 %_" + (this.state.getRegCounter()-3) + ", %_" + (this.state.getRegCounter()-2)
            +"\n\tbr i1 %_" + (this.state.getRegCounter()-1) + ", label %" + label[1] + ", label %" + label[0] + "\n\n" + label[0]
            +":\n\n\tcall void @throw_oob()\n\tbr label %" + label[1] + "\n\n" + label[1] + ":"); 
    }

    /*ArrayLookup:  f0 -> PrimaryExpression() [f2 -> PrimaryExpression()] */
    public String visit(ArrayLookup node){
        String id = node.f0.accept(this), index = node.f2.accept(this).split(" ")[1];

        // make sure array index is within bounds
        this.checkArrayIndex(id, index, true);

        // return a register holding the address of the index-th element 
        return this.getArrayElement(id, this.getArrayIndex(index));
    }

    /*ArrayLength:  f0 -> PrimaryExpression().length */
    public String visit(ArrayLength node){
        return this.getArrayElement(node.f0.accept(this), "0");
    }

    /* Arithmetic Expression Generic Function */
    public String arithmeticExpression(String left, String right, String op){
        String[] rightInfo = right.split(" ");
        emit("\n\t;apply arithmetic expression\n"
           + "\t" + this.state.newReg() + " = " + op + " " + left + ", " + rightInfo[1]); 
        return (op.equals("icmp slt") ? "i1" : "i32") + " %_" + (this.state.getRegCounter()-1);
    }

    /*CompareExpression
    * f0 -> PrimaryExpression() < f2 -> PrimaryExpression() */
    public String visit(CompareExpression node){
        return arithmeticExpression(node.f0.accept(this), node.f2.accept(this), "icmp slt");
    }

    /*PlusExpression
    * f0 -> PrimaryExpression() + f2 -> PrimaryExpression() */
    public String visit(PlusExpression node){
        return arithmeticExpression(node.f0.accept(this), node.f2.accept(this), "add");
    }

    /*MinusExpression
    * f0 -> PrimaryExpression() - f2 -> PrimaryExpression() */
    public String visit(MinusExpression node){
        return arithmeticExpression(node.f0.accept(this), node.f2.accept(this), "sub");
    }

    /*TimesExpression
    * f0 -> PrimaryExpression() * f2 -> PrimaryExpression() */
    public String visit(TimesExpression node){
        return arithmeticExpression(node.f0.accept(this), node.f2.accept(this), "mul");
    }

    /*AndExpression f0 -> Clause() &&  f2 -> Clause() */
    public String visit(AndExpression node){
        String leftReg = node.f0.accept(this), rightReg; // evaluate left side expression
        String labels[] = this.state.newLabel("and");
        emit("\t;short-circuit and clause, right side gets evaluated if and only if left side evaluates to true\n"
            +"\tbr " + leftReg + ", label %" + labels[0] + ", label %" + labels[1] + "\n\n" + labels[0] +":\n\t"); 
        rightReg = node.f2.accept(this); 
        emit("\tbr label %" + labels[2] + "\n\n" + labels[1] + ":\n\n\tbr label %" + labels[2] + "\n\n" + labels[2] + ":\n\n" 
            +"\t" + this.state.newReg() + " = phi i1 [" + rightReg.split(" ")[1] + ", %" + labels[0] + "],"
            + "[" + leftReg.split(" ")[1] + ", %" + labels[1] + "]");
        return "i1 %_" + (this.state.getRegCounter()-1);
    }

    /*Clause: f0 -> NotExpression() | PrimaryExpression() */
    public String visit(Clause node){
        return node.f0.accept(this);	
    }

    /*PrimaryExpression
    * f0 -> IntegerLiteral() | TrueLiteral() | FalseLiteral() | Identifier() 
    | ThisExpression() | ArrayAllocationExpression() | AllocationExpression() | BracketExpression() */
    public String visit(PrimaryExpression node){
        String child = node.f0.accept(this);
        State.IdInfo id;

        // in case of an identifier, return a register holding the CONTENT of the id
        if (node.f0.which == 3 ){
            id = this.state.getIdInfo(child);

            // if it is a field, load it to a register and return
            if(id == null)
                return this.getField(child, true);

            // else if it is a local variable or a parameter, there has been a previous allocation/store, 
            emit("\n\t;loading local variable '" +  child + "' from stack\n\t" 
                    + this.state.newReg() + " = load " + id.type + ", " + id.type + "* " + id.register);
            return id.type + " %_" + (this.state.getRegCounter()-1);

        }

        // else return type and value all in one
        return child;
    }

    /* Type: f0 -> ArrayType() | BooleanType() | IntegerType() | Identifier() */
    public String visit(Type node){
        return node.f0.accept(this);
    }

    /* Return each primitive type of MiniJava(int, int [] and boolean) as a String */ 
    public String visit(ArrayType node){
        return "array";
    }

    public String visit(BooleanType node){
        return "boolean";
    }

    public String visit(IntegerType node){
        return "integer";
    }

    /*Identifier:   f0 -> <IDENTIFIER>*/
    public String visit(Identifier node){
        return node.f0.toString();
    }

    /*IntegerLiteral:   f0 -> <INTEGER_LITERAL> */
    public String visit(IntegerLiteral node){
        return "i32 " + node.f0;
    }

    /*TrueLiteral:  f0 -> "true" */
    public String visit(TrueLiteral node){
        return "i1 1";
    }

    /*FalseLiteral: f0 -> "false" */
    public String visit(FalseLiteral node){
        return "i1 0";
    }

    /*ThisExpression:   f0 -> "this" */ 
    public String visit(ThisExpression node){  
        return "i8* %this";
    }    

    /*ArrayAllocationExpression:    new integer [f3 -> Expression()] */
    public String visit(ArrayAllocationExpression node){
        String size = node.f3.accept(this).split(" ")[1];
        emit("\n\t;allocate space for new array of size " + size + " + 1 place to store size at\n\t" 
            + this.state.newReg() + " = add i32 " + size + ", 1\n"
            +"\t" + this.state.newReg() + " = call i8* @calloc(i32 4, i32 %_" + (this.state.getRegCounter()-2) + ")\n" 
            +"\t" + this.state.newReg() + " = bitcast i8* %_" + (this.state.getRegCounter()-2) + " to i32*\n"
            +"\n\t;store size at index 0\n\tstore i32 " + size + ", i32* %_" + (this.state.getRegCounter()-1));      
        return "i32* %_" + (this.state.getRegCounter()-1);
    }

    /*AllocationExpresion:  new f1 -> Identifier()() */
    public String visit(AllocationExpression node){
        String className = node.f1.accept(this), tableSize;
        ClassData data = this.data.get(className);
        tableSize = "[" + data.methods.size() + " x i8*]";

        emit("\n\t;allocate space for a new \"" + className + "\" object\n\t" + this.state.newReg() + " = call i8* @calloc(i32 1, i32 " + data.size + ")"
            +"\n\t" + this.state.newReg() + " = bitcast i8* %_" + (this.state.getRegCounter()-2) + " to i8***\n"
            +"\t" + this.state.newReg() + " = getelementptr " + tableSize + ", " + tableSize + "* @." + className + "_vtable, i32 0, i32 0\n"
            +"\tstore i8** %_" + (this.state.getRegCounter()-1) + ", i8*** %_" + (this.state.getRegCounter()-2));
        return "i8* %_" + (this.state.getRegCounter()-3);
    }

    /*NotExpression:    f1 -> Clause() */
    public String visit(NotExpression node){
        String clause = node.f1.accept(this);
        emit("\n\t;apply logical not, using xor\n\t" + this.state.newReg() + " = xor " + clause + ", 1");
        return "i1 %_" + (this.state.getRegCounter()-1);
    }

    /*BracketExpression:    ( f1 -> Expression() )*/
    public String visit(BracketExpression node){
        return node.f1.accept(this);
    }    
}