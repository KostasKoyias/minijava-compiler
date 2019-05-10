import javafx.util.Pair;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap; 

/* for each class, store some meta data */
public class MethodData{
    String returnType;
    Integer offset;
    ArrayList<Pair<String, String>> arguments;

    MethodData(String returnType, Integer offset, ArrayList<Pair<String, String>> arguments){
        this.returnType = returnType;
        this.offset = offset;
        this.arguments = arguments;
    }
}