import javafx.util.Pair;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap; 

/* for each method, store some meta data */
public class MethodData{
    String className;       // whose class implementation it is
    String returnType;
    Integer offset;
    ArrayList<Pair<String, String>> arguments;

    MethodData(String className, String returnType, Integer offset, ArrayList<Pair<String, String>> arguments){
        this.className = className;
        this.returnType = returnType;
        this.offset = offset;
        this.arguments = arguments;
    }
}