import javafx.util.Pair;
import java.util.Map;
import java.util.LinkedHashMap; 

/* for each class, store some meta data */
public class ClassData{
    String parentName;
    Map <String, Pair<String, Integer>> vars;       // records of form: (variable_name, (type, offset))
    Map <String, MethodData> methods;    // records of form: (function_name, (return_type, offset, argTypes))
    public static final Integer pointerSize = 8;

    /* map all mini java data types to their actual size in bytes */
    private static final Map<String, Pair<Integer,String>> sizes = new LinkedHashMap<String, Pair<Integer,String>>() {
        private static final long serialVersionUID = 1L;
        {
            put("boolean", new Pair(1, "i1"));
            put("integer", new Pair(4, "i32"));
            put("array", new Pair(8, "i8*"));
        }
    };

    public ClassData(String parent){
        this.parentName = parent;
        this.vars = new LinkedHashMap<String, Pair<String, Integer>>();
        this.methods = new LinkedHashMap<String, MethodData>();
    }

    public static final Pair<Integer, String> getSize(String type){
        if(ClassData.sizes.containsKey(type))
            return ClassData.sizes.get(type);
        else 
            return new Pair(ClassData.pointerSize, "i8*");
    }

    /* given a variable_name to (type, offset) map, calculate the exact memory address for the next variable to be stored */
    public int getOffsetOfNextVar(){
        int offset = 0;
        String type;

        /* run through each variable entry and sum up the sizes */
        for(Map.Entry<String, Pair<String, Integer>> var: this.vars.entrySet()){
            type = var.getValue().getKey();
            offset += ClassData.getSize(type).getKey();
        }
        return offset + ClassData.pointerSize;
    }
}