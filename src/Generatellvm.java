import java.io.*;
import syntaxtree.*;
import javafx.util.Pair;
import java.util.LinkedHashMap; 
import visitor.GJNoArguDepthFirst;
import java.util.*;

public class Generatellvm extends GJNoArguDepthFirst<String>{
    private BufferedWriter out;
    protected Map<String, ClassData> data;
    private String className;
    private State state; 

    // Constructor: set a pointer to output file and set class data collected during the first pass
    Generatellvm(BufferedWriter out, Map<String, ClassData> data){
        this.out = out;
        this.data = data;
        this.state = new State();
    }

    // given a register or an integer add 1 to it because 1st place of an array is reserved for the length to be stored at, return the result
    private String getArrayIndex(String index){

        // if a register was passed, add 1 to the it and store the result in a register who will be returned
        if(index.startsWith("%_")){
            emit("\n\t" + this.state.newReg() + " = add i32 " + index + ", 1\n");
            return "%_" + (this.state.getRegCounter()-1);
        }

        // else index is a number, just add 1 to it and return the result
        return String.valueOf(Integer.parseInt(index) + 1);
    }

    // given a field of a class, return a register holding either the address or the content of the field
    private String getField(String field, boolean wantContent){
        Pair<String, Integer> fieldInfo = this.data.get(this.className).vars.get(field);
        String reg = this.state.newReg(field, "i8*", true), llvmType = ClassData.getSize(fieldInfo.getKey()).getValue(); 

        // set a pointer to the field
        emit("\n\t;load " + (wantContent ? "field " : "address of ") + this.className + "." + field + " from memory" 
            + "\n\t" + reg + " = getelementptr i8, i8* %this, i32 " + fieldInfo.getValue());

        // cast field pointer to actual size of field if it it is different than i8
        if(!"i8".equals(llvmType))
            emit("\t" + this.state.newReg(field, llvmType, true) + " = bitcast i8* " + reg + " to " + llvmType + "*");

        // if the actual content of the field was requested, load it to a register that will be returned 
        if(wantContent)
            emit("\t" + this.state.newReg(field, llvmType, false) + " = load " + llvmType + ", " + llvmType + "* " + ("%_" + (this.state.getRegCounter()-2)));
        return "%_" + (this.state.getRegCounter()-1);
    }

    // given an identifier return a pair containing the register that holds the address of the id, and the type of the id
    private String getIdAddress(String id){
        State.Info info = this.state.getInfo(id);

        // if identifier is a field of this class
        if(info == null){
            this.getField(id, false);
            info = this.state.getInfo(id);
        }
        // else this identifier is already stored, get a register holding the ADDRESS of the identifier
        else if(!info.isLocal)
                info.reg = info.reg.replace("%.", "%");
        info.type += "*";
        return info.type + " " + info.reg;
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
            +    "\tret void\n}\n");
        
        // visit main
        node.f0.accept(this);

        // visit all user-defined classes 
        for(int i = 0; i < node.f1.size(); i++)
            node.f1.elementAt(i).accept(this);
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
   		
   		for (int i = 0; i < node.f14.size(); i++)
 		   node.f14.elementAt(i).accept(this);

   		for (int i = 0; i < node.f15.size(); i++)
               node.f15.elementAt(i).accept(this);
	    emit("\tcall void (i32) @print_int(i32 23)");
        
        
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

        for (int i = 0; i < node.f4.size(); i++)
            node.f4.elementAt(i).accept(this);
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

        for (int i = 0; i < node.f6.size(); i++)
            node.f6.elementAt(i).accept(this);
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
        this.state.put(id, "%" + id, varType, true);
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
        
        // allocate space and store each parameter of the method
        String llvmType, paramID;
        if(parameters != null){
            emit("\t;allocate space and store each parameter of the method");
            for(Pair<String, String> par : parameters){
                paramID = par.getValue();
                llvmType = ClassData.getSize(par.getKey()).getValue();
                emit("\t%" + paramID + " = alloca " + llvmType +
                     "\n\tstore " + llvmType + " %." + paramID + ", " + llvmType + "* %" + paramID);
                this.state.put(paramID, "%." + paramID, llvmType, false);
            } 
        }  

        // visit variable declarations
        for (int i = 0; i < node.f7.size(); i++)
            node.f7.elementAt(i).accept(this);
        
        // visit statements 
        for (int i = 0; i < node.f8.size(); i++)
            node.f8.elementAt(i).accept(this);

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
        for (int i = 0; i < node.f1.size(); i++)
            node.f1.elementAt(i).accept(this);
        return null;
    }

    /*  Assignment Statement:   f0 -> Identifier() = f2 -> Expression(); */
    public String visit(AssignmentStatement node){ 
        String leftID = node.f0.accept(this), rightSide = node.f2.accept(this), leftType, leftReg, rightType = rightSide.split(" ")[0];
        String[] leftInfo = this.getIdAddress(leftID).split(" "); 
        leftType = leftInfo[0]; leftReg = leftInfo[1];

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

        // load a pointer to the array, cast it to integer pointer, get it to point at the index-th element and modify it
        emit("\n\t;assign a value to the array element\n\t" + this.state.newReg(leftID, "i8*", false) + " = load i8*, " + leftInfo + "\n\t"
            + this.state.newReg() + " = bitcast i8* %_" + (this.state.getRegCounter()-2) + " to i32*\n\t"
            + this.state.newReg() + " = getelementptr i32, i32* %_" + (this.state.getRegCounter()-2) + " , i32 " + this.getArrayIndex(index)
            + "\n\tstore " + rightSide +", i32* %_" + (this.state.getRegCounter()-1));
        return null;
    }

    /*Print Statement: System.out.println( f2 -> Expression());*/
	public String visit(PrintStatement node){
		String expr = node.f2.accept(this);
		emit("\tcall void (i32) @print_int(" + expr +")");
		return null;

	}

    /*Expression
    * f0 -> AndExpression() | CompareExpression() | PlusExpression() | MinusExpression() 
        | TimesExpression() | ArrayLookup() | ArrayLength() | MessageSend() | Clause()
    */
    public String visit(Expression node){
        return node.f0.accept(this);
    }

    /*ArrayLookup:  f0 -> PrimaryExpression() [f2 -> PrimaryExpression()] */
    public String visit(ArrayLookup node){
        String id = node.f0.accept(this), index = node.f2.accept(this).split(" ")[1];

        emit("\n\t;lookup *(" + id.split(" ")[1] + " + " + index + ")\n"  
            +"\t" + this.state.newReg() + " = bitcast " + id + " to i32*\n"
            +"\t" + this.state.newReg() + " = getelementptr i32, i32* %_" + (this.state.getRegCounter()-2) + ", i32 " + this.getArrayIndex(index)
            +"\n\t" + this.state.newReg() + " = load i32, i32* %_" + (this.state.getRegCounter()-2));    
        return "i32 %_" + (this.state.getRegCounter()-1);
    }

    /*ArrayLength:  f0 -> PrimaryExpression().length */
    public String visit(ArrayLength node){
        return "later";
    }

    /* Arithmetic Expression Generic Function */
    public String arithmeticExpression(String left, String right, String op){
        String[] rightInfo = right.split(" ");
        emit("\n\t;apply arithmetic expression\n"
           + "\t" + this.state.newReg() + " = " + op + " " + left + ", " + rightInfo[1]); 
        return (op.equals("icmp slt") ? "i1" : "i32") + " %_" + (this.state.getRegCounter()-1);
    }

    /*AndExpression f0 -> Clause() &&  f2 -> Clause() */
    public String visit(AndExpression node){
        return arithmeticExpression(node.f0.accept(this), node.f2.accept(this), "and");   
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

    /*Clause: f0 -> NotExpression() | PrimaryExpression() */
    public String visit(Clause node){
        return node.f0.accept(this);	
    }

    /*PrimaryExpression
    * f0 -> IntegerLiteral() | TrueLiteral() | FalseLiteral() | Identifier() 
    | ThisExpression() | ArrayAllocationExpression() | AllocationExpression() | BracketExpression() */
    public String visit(PrimaryExpression node){
        String child = node.f0.accept(this);
        State.Info id;

        // in case of an identifier, return a register holding the CONTENT of the id
        if (node.f0.which == 3 ){
            id = this.state.getInfo(child);

            // if it is a field, load it to a register and return type and register after if statement
            if(id == null){
                this.getField(child, true);
                id = this.state.getInfo(child);
            }
            // else if it is al local variable, there has been a previous allocation/store, so load the content and return 
            else if(id.isLocal){
                emit("\n\t;loading local variable\n\t" + this.state.newReg() + " = load " + id.type + ", " + id.type + "* " + id.reg);
                return id.type + " %_" + (this.state.getRegCounter()-1);
            }

            // else it is a parameter or an already loaded variable, so return it as it is, no need to load
            return id.type + " " + id.reg;
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
        return "%this";
    }    

    /*ArrayAllocationExpression:    new integer [f3 -> Expression()] */
    public String visit(ArrayAllocationExpression node){
        String size = node.f3.accept(this).split(" ")[1];
        emit("\n\t;allocate space for new array of size " + size + " + 1 place to store size at\n\t" 
            + this.state.newReg() + " = add i32 " + size + ", 1\n"
            +"\t" + this.state.newReg() + " = call i8* @calloc(i32 1, i32 %_" + (this.state.getRegCounter()-2) + ")\n" 
            +"\t" + this.state.newReg() + " = bitcast i8* %_" + (this.state.getRegCounter()-2) + " to i32*\n"
            +"\n\t;store size at index 0\n\tstore i32 " + size + ", i32* %_" + (this.state.getRegCounter()-1));      
        return "i32* %_" + (this.state.getRegCounter()-1);
    }

    /*AllocationExpresion:  new f1 -> Identifier()() */
    public String visit(AllocationExpression node){
        String className = node.f1.accept(this);
        return className;
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