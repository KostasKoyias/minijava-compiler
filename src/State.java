import java.io.*;
import syntaxtree.*;
import java.util.LinkedHashMap; 
import java.util.*;

/* this class holds all information about the identifiers and registers a class uses */
public class State{
    private Map<String, String> ids;    // a map from id to reg
    private Map<String, String> regs;   // a map from reg to type

    State(){
        this.ids = new LinkedHashMap<String, String>();
        this.regs = new LinkedHashMap<String, String>();
    }

    public void put(String id, String reg, String type){
        this.ids.put(id, reg);
        this.regs.put(reg, type);
    }

    public String getReg(String id){
        return this.ids.get(id);
    }

    public String getType(String idOrReg){
        if(this.ids.containsKey(idOrReg))
            return this.ids.get(idOrReg);
        else if(this.regs.containsKey(idOrReg))
            return this.regs.get(idOrReg);
        else 
            return null;
    }

    public void clear(){
        this.ids.clear(); this.regs.clear();
    }
}