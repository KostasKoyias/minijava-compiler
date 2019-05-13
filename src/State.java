import java.io.*;
import syntaxtree.*;
import java.util.LinkedHashMap; 
import java.util.*;


/* this class holds all information about the identifiers a method uses */
public class State{
    private Map<String, Info> ids; 
    private int regCounter;
    private Statement[] statements;
    private static int statementsNumber = 3;

    // nested class Info holding all info needed for a given identifier
    class Info{
        String reg;
        String type;
        boolean isLocal;

        Info(String reg, String type, boolean isLocal){
            this.reg = reg; this.type = type; this.isLocal = isLocal;
        }

        public Info clone(){  
            return new Info(this.reg, this.type, this.isLocal);  
        }  
    }

    class Statement{
        int counter;
        String[] labels;

        Statement(String[] labels){
            this.labels = labels;
            this.counter = 0;
        }

        public String[] getLabels(){
            int len = this.labels.length;
            String[] rv =  new String[len];
            for(int i = 0; i < len ; i++)
                rv[i] = this.labels[i] + "_" + this.counter;
            this.counter++;            
            return rv;
                
        }

        public void resetCounter(){
            this.counter = 0;
        }
    }

    // map all kinds of labels to be used to a code
    private static final Map<String, Integer> labelTypes = new LinkedHashMap<String, Integer>() {
        private static final long serialVersionUID = 1L;
        {
            put("if", 0);
            put("while", 1);
            put("oob", 2);
        }
    }; 

    // Constructor: initialize identifier map and counters
    State(){
        this.ids = new LinkedHashMap<String, Info>();
        this.regCounter = 0;
        this.statements = new Statement[State.statementsNumber];
        this.statements[0] = new Statement(new String[] {"if", "else", "fi"});
        this.statements[1] = new Statement(new String[] {"while", "do", "done"});
        this.statements[2] = new Statement(new String[] {"outOfBounds", "withinBounds"});
    }

    // return next register available, update method's state by associating the identifier to the register, a data-type and a "is pointer" field  
    public String newReg(){
        return "%_" + this.regCounter++;
    }

    public String newReg(String id, String llvmType, boolean isLocal){
	String regCurr = String.valueOf("%_" + this.regCounter), identifier = id == null ? regCurr : id;
        this.put(identifier, regCurr, llvmType, isLocal);
        return this.newReg();
    }

    // insert information about a new identifier used by this method
    public void put(String id, String reg, String type){
        this.put(id, reg, type, true);
    }

    public void put(String id, String reg, String type, boolean isLocal){
        Info info = new Info(reg, type, isLocal);
        this.ids.put(id, info);
    }

    // get a new mutable version of all information about an identifier
    public Info getInfo(String id){
        return this.ids.containsKey(id) ? this.ids.get(id).clone() : null;
    }

    // get just the type of an identifier
    public String getType(String id){
        if(this.ids.containsKey(id))
            return this.ids.get(id).type;
        else 
            return null;
    }

    public int getRegCounter(){
        return this.regCounter;
    }

    // get a new Statement of the type requested, label types are declare in a map called labels along with their id
    public String[] newLabel(String label){
        int index = State.labelTypes.containsKey(label) ? State.labelTypes.get(label) : -1;
        if(index == -1)
            return null;
        
        return this.statements[index].getLabels();
    }

    // reset state
    public void clear(){
        this.ids.clear();
        this.regCounter = 0;
        for(int i = 0; i < State.statementsNumber; i++)
            this.statements[i].resetCounter();
    }
}
