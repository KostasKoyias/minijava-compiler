/*  compile this MiniJava source file with MiniJava-Compiler to see how System.out.println
    is implemented to handle both integer and boolean values, having the exact same output
    as compiling with javac and running with java */ 
class PrintTest{
    public static void main(String[] args){
        System.out.println(5);
        System.out.println(5<0);
        System.out.println(0<5);
    }
}