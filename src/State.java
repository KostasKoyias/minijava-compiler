import java.io.*;
import syntaxtree.*;
import java.util.LinkedHashMap; 
import java.util.*;


/* this class holds all information about the identifiers a method uses */
public class State{
    private Map<String, IdInfo> ids; 
    private int regCounter;
    private Statement[] statements;

    // nested class IdInfo holding all information needed for a given identifier
    class IdInfo{
        String regAddress;
        String regContent;
        String type;

        IdInfo(String regAddress, String regContent, String type){
            this.regAddress = regAddress; this.regContent = regContent; this.type = type;
        }  

        public void clear(){
            this.regContent = null;
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
            put("and", 3);
        }
    }; 

    // Constructor: initialize identifier map and counters
    State(){
        this.ids = new LinkedHashMap<String, IdInfo>();
        this.regCounter = 0;
        this.statements = new Statement[State.labelTypes.size()];
        this.statements[0] = new Statement(new String[] {"if", "else", "fi"});
        this.statements[1] = new Statement(new String[] {"while", "do", "done"});
        this.statements[2] = new Statement(new String[] {"outOfBounds", "withinBounds"});
        this.statements[3] = new Statement(new String[] {"true", "false", "end"});
    }

    // return next register available 
    public String newReg(){
        return "%_" + this.regCounter++;
    }

    // when an identifier is loaded/stored to a register update method's state by associating the identifier to that register 
    public String newReg(String id, String llvmType, boolean isStore){
        String regCurr = String.valueOf("%_" + this.regCounter);
        
        // if the content of an identifier was altered, regContent is outdated, so update regAddress only 
        if(isStore)
            this.ids.put(id, new IdInfo(regCurr, null, llvmType));
        // else if an identifier was loaded, update regContent or insert a new registration because it was probably a field loaded
        else{
            if(this.ids.containsKey(id))
                this.ids.get(id).regContent = regCurr;
            else 
                this.put(id, regCurr, null, llvmType);
        }
        return this.newReg();
    }

    // insert information about a new identifier used by this method
    public void put(String id, String regAddress, String regContent, String llvmType){
        this.ids.put(id, new IdInfo(regAddress, regContent, llvmType));
    }

    // get a new mutable version of all information about an identifier
    public IdInfo getIdInfo(String id){
        return this.ids.containsKey(id) ? this.ids.get(id) : null;
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
        for(int i = 0; i < State.labelTypes.size(); i++)
            this.statements[i].resetCounter();
    }
}
