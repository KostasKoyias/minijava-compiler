class NestedLoops{
    public static void main(String[] args){
        System.out.println((new Base()).loop(1, 2)); // should print 17
    }
}

class Base{
    public int loop(int x, int y){    // parameters are already loaded in some register
        int i;
        int j;
        int z;
        z = 0;

        // these two loops should share the same loop-modified table
        i = 0;
        while(i < 2){
            i = i + 1;
            z = x + 1;      // although x is loaded, it has to be re-loaded, because it was modified in the previous iteration
            x = y + 1;      // see? now y has to be re-loaded, because it was modified in the INNER loop of the previous iteration
            j = 0; 
            while(j < 2){
                j = j + 1; 
                z = z + y;  // although y was loaded 4 lines above, it now has to be re-loaded because it was modified in the previous iteration of the OUTER loop
            }
            y = y * 2;      // see?
        }
        return x + z;       // so these two loops share the same table of loop-modified variables, which are on the left side of an assignment 
        // so these two loops should share the same table of loop-modified variables, which are on the left side of an assignment anywhere in those loops
        // of course this can be optimized, but loading all of these will certainly keep us "safe" 
    }
}