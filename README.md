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

Source code happens to be commented in maybe a little too much detail, so only important and complex parts of the
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
sudo apt update && sudo apt install clang-6.0
```

Then, after compiling the compiler(yeah I know) using the makefile under src directory
, produce intermediate code for any MiniJava source file by running Main.class under build directory. For example

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
actually works by failing at execution. Run it from any location inside the repository as follows:

```bash
MiniJava-Compiler/tests $ ./tester.sh
```

So this is it, hopefully I made things clear.  
