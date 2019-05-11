import java.io.*;
import syntaxtree.*;
import java.util.LinkedHashMap; 
import java.util.*;


/* this class holds all information about the identifiers a method uses */
public class State{

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
    private Map<String, Info> ids; 

    State(){
        this.ids = new LinkedHashMap<String, Info>();
    }

    public void put(String id, String reg, String type){
        this.put(id, reg, type, true);
    }

    public void put(String id, String reg, String type, boolean isLocal){
        Info info = new Info(reg, type, isLocal);
        this.ids.put(id, info);
    }

    public Info getInfo(String id){
        return this.ids.containsKey(id) ? this.ids.get(id).clone() : null;
    }

    public String getType(String id){
        if(this.ids.containsKey(id))
            return this.ids.get(id).type;
        else 
            return null;
    }

    public void clear(){
        this.ids.clear();
    }
}