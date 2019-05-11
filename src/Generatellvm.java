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
    private int regCounter;
    private State state; 

    // Constructor: set a pointer to output file and set class data collected during the first pass
    Generatellvm(BufferedWriter out, Map<String, ClassData> data){
        this.out = out;
        this.data = data;
        this.regCounter = 0;
        this.state = new State();
    }  

    // return next register available
    private String nextReg(String llvmType){
        this.state.put(null, String.valueOf("%_" + this.regCounter), llvmType);
        return "%_" + this.regCounter++;
    }

    // given a field of a class, return a register holding either the address or the content of the field
    private String getField(String field, boolean wantContent){
        Pair<String, Integer> fieldInfo = this.data.get(this.className).vars.get(field);
        String reg = this.nextReg("i8*"), llvmType = ClassData.getSize(fieldInfo.getKey()).getValue(); 

        // set a pointer to the field
        emit("\n\t;load " + (wantContent ? "field " : "address of ") + this.className + "." + field + " from memory" 
            + "\n\t" + reg + " = getelementptr i8, i8* %this, " + llvmType + " " + fieldInfo.getValue());

        // cast field pointer to actual size of field if it it is different than i8
        if(!"i8".equals(llvmType))
            emit("\t" + this.nextReg(llvmType + "*") + " = bitcast i8* " + reg + " to " + llvmType + "*");

        // if the actual content of the field was requested, load it to a register that will be returned 
        if(wantContent)
            emit("\t" + this.nextReg(llvmType) + " = load " + llvmType + ", " + llvmType + "* " + ("%_" + (this.regCounter-2)));
        return "%_" + (this.regCounter-1);
    }

    // given an identifier return a pair containing the register that holds the id, and the type of the id
    private Pair<String, String> getIdInfo(String id, boolean wantContent){
        String reg = this.state.getReg(id), type = null;

        // if identifier is a field of this class
        if(reg == null){
            reg = this.getField(id, wantContent);
            type = this.state.getType(reg);
        }
        // else if it is either a local variable or a parameter to be modified(wantContent), get a register holding the ADDRESS of the identifier
        else if(!wantContent){
            type = this.state.getType(reg) + "*";
            reg = reg.replace("%.", "%");
        }
        return new Pair(reg, type);
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
        this.state.put(id, "%." + id, varType);
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
        this.regCounter = 0;

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
                this.state.put(paramID, "%." + paramID, llvmType);
            } 
        }  

        // visit variable declarations
        for (int i = 0; i < node.f7.size(); i++)
            node.f7.elementAt(i).accept(this);
        
        // visit statements 
        for (int i = 0; i < node.f8.size(); i++)
            node.f8.elementAt(i).accept(this);

        emit("\tret i32 0\n}\n");        
        //emit("}\n");
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

    /*  Assignment Statement
        f0 -> Identifier() = f2 -> Expression();
    */
    public String visit(AssignmentStatement node){ 
        String leftID = node.f0.accept(this), rightSide = node.f2.accept(this), type;
        Pair<String, String> leftInfo = this.getIdInfo(leftID, false);

        // store the content of the address calculated for the right side, at the address calculated for the left side
        type = leftInfo.getValue();
        emit("\tstore " + rightSide + ", " + type + " " + leftInfo.getKey());
        return null;
    }

    /*Expression
    * f0 -> AndExpression() | CompareExpression() | PlusExpression() | MinusExpression() 
        | TimesExpression() | ArrayLookup() | ArrayLength() | MessageSend() | Clause()
    */
    public String visit(Expression node){
        return node.f0.accept(this);
    }

    /*Clause: f0 -> NotExpression() | PrimaryExpression() */
    public String visit(Clause node){
        return node.f0.accept(this);	
    }

    /*PrimaryExpression
    * f0 -> IntegerLiteral() | TrueLiteral() | FalseLiteral() | Identifier() 
    | ThisExpression() | ArrayAllocationExpression() | AllocationExpression() | BracketExpression() */
    public String visit(PrimaryExpression node){
        String child = node.f0.accept(this), reg = null, type = null;

        // in case of an identifier, it might be a (local variable or parameter) or a field of this class, return a register holding the CONTENT of the id
        if (node.f0.which == 3){
            reg = this.state.getReg(child);
            if(reg == null)
                reg = this.getField(child, true);
            type = this.state.getType(reg);
            return type + " " + reg;
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

    /*IntegerLiteral
    * f0 -> <INTEGER_LITERAL> */
    public String visit(IntegerLiteral node){
        return "i32 " + node.f0.accept(this);
    }

    /*TrueLiteral
    * f0 -> "true" */
    public String visit(TrueLiteral node){
        return "i1 1";
    }

    /*FalseLiteral
    * f0 -> "false" */
    public String visit(FalseLiteral node){
        return "i1 0";
    }

    /*ThisExpression
    * f0 -> "this" */ 
    public String visit(ThisExpression node){  
        return "%this";
    }    

    /*Identifier
    * f0 -> <IDENTIFIER>*/
    public String visit(Identifier node){
        return node.f0.toString();
    }
}