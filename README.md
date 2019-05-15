# MiniJava-Compiler

## Introduction

This compiler aims to generate intermediate code used by the [LLVM](https://llvm.org/docs/LangRef.html#instruction-reference) compiler for a series of
[MiniJava](http://cgi.di.uoa.gr/~thp06/project_files/minijava-new/minijava.html), a pure subset of Java,  input files. To accomplish that, two visitors
extending GJDepthFirstVisitor of JTB were implemented.  
First of, all necessary information
about each class of the file was collected.  
Based on those, the second visitor produced the intermediate code.  
These files need to be [**syntactically**](http://cgi.di.uoa.gr/~thp06/project_files/minijava-new/minijava.jj)
and **semantically** correct.  

## Visitors

### FirstVisitor.java

This visitor gathered all significant data for each class, using a map from class name to a structure holding
information about the fields and member methods of the class, along with other meta data, like the name of the parent class.  
For each **field**, it is vital to know at which offset from the class pointer it was stored, so that it can then be loaded.  
Similarly, each **method** has to be enumerated, in order to load the right method pointer
from the v-table later on.

### Generatellvm.java

All information gathered during the first pass, where later passed on to this visitor.
Based on those, this visitor emitted all intermediate code into the output .ll file.

## Implementation Details

Source code happens to commented in maybe too a little too much detail, so only important and complex parts of the
implementation will be pointed out here.

### MessageSend

To keep track of each [MessageSend](http://cgi.di.uoa.gr/~thp06/project_files/minijava-new/minijava.html#prod31),
a FIFO queue was used, where the class name of each method called was inserted,
after it was evaluated. When a message send was encountered during the second pass,
the visitor popped the class name from the queue and got all information about
the method from the symbol table. Both visitors extend GJDepthFirst, so they visit nodes in the same order.

### AssignmentStatement

This node is interesting. Notice that an instance of type B, where B extends A can
replace an A instance at any time. This is called
[*Liskov substitution*](https://en.wikipedia.org/wiki/Liskov_substitution_principle)
and MiniJava happens to support strong behavioral subtyping. This node takes care of that,
by updating the fields map of the class during the first pass in case of assigning
an instance of B to a variable that refers to an A.

### Load once, use many

In order to minimize loading, a once loaded identifier was not re-loaded until modified.
To accomplish that, a map was used for each method associating each identifier <sup>1</sup> to a pair of registers.
First of them, holding the address and the other holding the content.

In case of an assignment, right side identifier was only loaded if it was either modified
or registerContent was empty. Left side identifier, cleared his own registerContent
after that.  
<sup>1</sup> This could be a:

* parameter
* local variable or
* a class field

#### While loops

As you might just thought, some extra info need to be collected about a while loop to avoid
using the same content for an loop-modified variable. For example

```java
x = 0;
y = x;
while(y < 5){
    y = y + 1;
    x = x + 5;    // load or not?
}
```

in the above code x is loaded to be assigned to y. Then, in order to assign x + 1 to x,
one might think that content is already loaded because of the previous assignment.
Well, that one would only be right in case the loop was executed just once.
But, this is usually not the case with loops, so each loop-modified variable
is re-loaded. Or else, x would be assigned 0 + 5 in all 5 loops.

To detect loop-modified variables, a FIFO queue was used during the first pass,
containing a single loopTable(HashSet) for each outermost loop.
This was initialized by the outer
[WhileStatement](http://cgi.di.uoa.gr/~thp06/project_files/minijava-new/minijava.html#prod22)
and updated by [AssignmentStatement](http://cgi.di.uoa.gr/~thp06/project_files/minijava-new/minijava.html#prod19).
Notice that each outer loop, actually shares the same loop-modified table with all nested loops
inside of it. This holds, just because in MiniJava each function opens up a new scope,
and all variables are declared in the beginning of it, so all loops actually share the same variables.  
Check out *tests/in/NestedLoops.java* example to fully understand this.

### Array outOfBounds check

When it comes to arrays, intermediate code always makes sure an array index
is within the appropriate range and throws an exception in any other case,
preventing a Segmentation Violation. To pull that off, the length of each array is
stored at index 0 and loaded in each lookup to perform that check.

### Short-Circuit &&

It is common to use && in place of an if statement. For example

```java
if(map != null && map.containsKey(key))
```

No one wants to use a method of a null pointer so the right side sub-expression of this
[AndExpression](http://cgi.di.uoa.gr/~thp06/project_files/minijava-new/minijava.html#prod24)
should only be evaluated in case the left one is evaluated to true.
Intermediate code takes care of that using some conditional branching and a very useful
LLVM command [phi](https://llvm.org/docs/LangRef.html#phi-instruction), which allows one to
select the right value in case of conditional branching, based on which path was eventually taken.

### Compile and run

In order to compile MiniJava files one needs Clang with version >= 4.0.0.  
This can be installed, in Ubuntu Trusty as follows:

```bash
sudo apt update && sudo apt install clang-4.0
```

Then, after compiling the compiler(yeah I know), using the makefile under src directory
produce intermediate code running Main.class under build directory. For example

``` bash
MiniJava-Compiler $ cd src
MiniJava-Compiler/src $ make
MiniJava-Compiler/src $ cd ../build
MiniJava-Compiler/build $ java Main ../tests/in/LinkedList.java
```

>To view field offsets for each class, along with the index of each member-method rerun with
**--offsets**.  
Notice that child classes share the same index for member-methods, because it is actually
the same function and it is stored in memory just once, if not overriden.

Of course, one can run Main for a series of files like:

```bash
MiniJava-Compiler/build $ java Main ../tests/in/LinkedList.java ../tests/in/Factorial.java
```

The above will generate intermediate code into file LinkedList.ll
under the same directory as LinkedList.java.
Change into tests/in directory and execute it as follows:

```bash
clang-4.0 -o out LinkedList.ll
./out
```

Try and run the same source files using javac and java.

### Testing

Of course a pretty cool test script was added, written in bash. One can find it under directory *tests*.
It generates, compiles and runs intermediate code for each .java file under *tests/in*.
A complete example highlighting all properties of MiniJava-Compiler is the one name *MyExample.java*.
Another interesting one is *OutOfBounds.java* which verifies that the array index check mentioned above
actually works. Run it from any directory of level 1(src, tests or build).

So that is it, hopefully I made things clear.  
