import java.io.*;
import syntaxtree.*;
import java.util.LinkedHashMap; 
import java.util.*;


/* this class holds all information about the identifiers a method uses */
public class State{
    private Map<String, Info> ids; 
    int regCounter;
    int[] labelsCounter = new int[2];

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

    // map all labels to be used to a code
    private static final Map<String, Integer> labelTypes = new LinkedHashMap<String, Integer>() {
        private static final long serialVersionUID = 1L;
        {
            put("oob", 0);
            put("alloca", 1);
        }
    }; 

    // Constructor: initialize identifier map and counters
    State(){
        this.ids = new LinkedHashMap<String, Info>();
        this.regCounter = 0;
    }

    // return next register available, update method's state by associating the identifier to the register, a data-type and a "is pointer" field  
    public String newReg(){
        return "%_" + this.regCounter++;
    }

    public String newReg(String llvmType, boolean isLocal){
        return this.newReg(null, llvmType, isLocal);
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

    // get a new label of the type requested, label types are declare in a map called labels along with their id
    public String getLabel(String label){
        return State.labelTypes.containsKey(label) ? label + "_" + (this.labelsCounter[State.labelTypes.get(label)]++) + ": " : null;
    }

    // reset state
    public void clear(){
        this.ids.clear();
        this.regCounter = 0;
        Arrays.fill(this.labelsCounter, 0);
    }
}
