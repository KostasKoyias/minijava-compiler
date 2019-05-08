all: compile

compile:
	java -jar ../jtb132di.jar minijava.jj
	java -jar ../javacc5.jar minijava-jtb.jj
	javac Main.java 

clean:
	rm -rf *.class *~ *Parse* *Token* *jtb* *CharStream* syntaxtree visitor