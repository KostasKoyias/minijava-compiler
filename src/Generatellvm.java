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
    private int regs;
    private ArrayList<Pair<String, String>> scope;

    // Constructor: set a pointer to output file and set class data collected during the first pass
    Generatellvm(BufferedWriter out, Map<String, ClassData> data){
        this.out = out;
        this.data = data;
        this.regs = 0;
        this.scope = new ArrayList<Pair<String, String>>();
    }  

    private String nextReg(){
        return "%_" + this.regs++;
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
        
        
        emit("\tret i32 0\n}");
   		return null;
    }

    /*Type Declaration
      f0 -> ClassDeclaration()    |   ClassExtendsDeclaration() */
    public String visit(TypeDeclaration node){
        node.f0.accept(this);
        this.regs = 0;
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

    /*
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
        emit("\t%" + id + " = alloca " + varType);
        this.scope.add(new Pair("%" + id, varType));
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
        String returnType, id = node.f2.accept(this);
        Pair<Integer, String> type = ClassData.getSize(node.f1.accept(this));
        returnType = (type != null ? type.getValue() : "i8*");

        // emit method's signature
        ArrayList<Pair<String, String>> parameters = this.data.get(this.className).methods.get(id).arguments;
        emit("define " + returnType + " @" + this.className + "." + id + MyUtils.getArgs(parameters, true) + "{");	  
        
        // allocate space and store each parameter of the method
        String llvmType, paramID;
        if(parameters != null){
            for(Pair<String, String> par : parameters){
                paramID = par.getValue();
                llvmType = ClassData.getSize(par.getKey()).getValue();
                emit("\t%" + paramID + " = alloca " + llvmType +
                    "\n\tstore " + llvmType + " %." + paramID + ", " + llvmType + "* %" + paramID);
                this.scope.add(new Pair("%" + paramID, llvmType));
            } 
        }  

        // visit variable declarations
        for (int i = 0; i < node.f7.size(); i++)
            node.f7.elementAt(i).accept(this);
        
        // visit statements 
        for (int i = 0; i < node.f8.size(); i++)
            node.f8.elementAt(i).accept(this);

        emit("}\n");
        this.scope.clear();
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
        String left = node.f0.accept(this), right = node.f2.accept(this);
        Pair<String, String> leftInfo = MyUtils.getReg(this.scope, left), rightInfo = MyUtils.getReg(this.scope, right);

        String rightReg = this.nextReg(), rightType, tempReg;
        Pair<String, Integer> rightField;
        // if the right side operand is either a parameter or a local variable, load it's content using the (pointer, type) pair returned by MyUtils.getReg 
        if(rightInfo != null){
            rightType = rightInfo.getValue();
            emit("\t" + rightReg + " = load " + rightType + ", " + rightType + "* " +  rightInfo.getKey());
        }
        //else it is a field of the current class
        else{
            rightField = this.data.get(this.className).vars.get(right);
            rightType = ClassData.getSize(rightField.getKey()).getValue();
            emit("\t" + rightReg + " = getelementptr i8, i8* %this, " + rightType + " " + rightField.getValue());

            // cast right side operand pointer to actual size of field 
            if(!"i8".equals(rightType)){
                tempReg = rightReg;
                rightReg = this.nextReg();
                emit("\t" + rightReg + " = bitcast i8* " + tempReg + " to " + rightType);
            }
        }

        String leftReg, leftType;
        Pair<String, Integer> leftField;
        // if the left side operand is either a parameter or a local variable, store the result at the address returned by MyUtils.getReg
        if(leftInfo != null)
            emit("\tstore " + rightType + " " + rightReg + ", " + rightType + "* " + leftInfo.getKey());
        // else if it is a field, get it's (type, offset) pair from the lookUpTable built during the 1st pass
        else{
            leftField = this.data.get(this.className).vars.get(left);
            leftReg = this.nextReg();
            leftType = ClassData.getSize(leftField.getKey()).getValue();
            emit("\t" + leftReg + " = getelementptr i8, i8* %this, " + leftType + " " + leftField.getValue());

            // cast left side operand pointer to actual size of field 
            if(!"i8".equals(leftType)){
                tempReg = leftReg;
                leftReg = this.nextReg();
                emit("\t" + leftReg + " = bitcast i8* " + tempReg + " to " + leftType);
            }
            emit("\tstore " + rightType + " " + rightReg + ", " + leftType + "* " + leftReg);
        }



       
        return null;

    }

    /*Expression
    * f0 -> AndExpression() | CompareExpression() | PlusExpression() | MinusExpression() 
        | TimesExpression() | ArrayLookup() | ArrayLength() | MessageSend() | Clause()
    */
    public String visit(Expression node){
        return node.f0.accept(this);
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
    
    /*Identifier
    * f0 -> <IDENTIFIER>*/
    public String visit(Identifier node){
        return node.f0.toString();
    }
}