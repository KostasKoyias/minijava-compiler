;for each class, declare a global vTable containing a pointer for each method
@.Base_vtable = global [2 x i8*] [i8* bitcast (i32 (i8*, i32)* @Base.set to i8*), i8* bitcast (i32 (i8*)* @Base.get to i8*)]
@.Derived_vtable = global [3 x i8*] [i8* bitcast (i32 (i8*, i32)* @Derived.set to i8*), i8* bitcast (i32 (i8*)* @Base.get to i8*), i8* bitcast (i1 (i8*)* @Derived.myMethod to i8*)]

;declare functions to be used
declare i8* @calloc(i32, i32)
declare i32 @printf(i8*, ...)
declare void @exit(i32)

;define constants and functions to be used
@_cint = constant [4 x i8] c"%d\0a\00"
@_cOOB = constant [15 x i8] c"Out of bounds\0a\00"
define void @print_int(i32 %i) {
	%_str = bitcast [4 x i8]* @_cint to i8*
	call i32 (i8*, ...) @printf(i8* %_str, i32 %i)
	ret void
}

define void @throw_oob() {
	%_str = bitcast [15 x i8]* @_cOOB to i8*
	call i32 (i8*, ...) @printf(i8* %_str)
	call void @exit(i32 1)
	ret void
}

define i32 @main() {

	;allocate space for local variable %r
	%r = alloca i8*

	;allocate space for local variable %len
	%len = alloca i32

	;allocate space for new array of size 13 + 1 place to store size at
	%_0 = add i32 13, 1
	%_1 = call i8* @calloc(i32 1, i32 %_0)
	%_2 = bitcast i8* %_1 to i32*

	;store size at index 0
	store i32 13, i32* %_2

	;adjust pointer type of left operand
	%_3 = bitcast i8** %r to i32**

	;store result
	store i32* %_2, i32** %_3

	;loading local variable
	%_4 = load i8*, i8** %r

	;get length of array at %_4
	%_5 = bitcast i8* %_4 to i32*
	%_6 = getelementptr i32, i32* %_5, i32 0
	%_7 = load i32, i32* %_6

	;store result
	store i32 %_7, i32* %len

	;loading local variable
	%_8 = load i32, i32* %len

	;apply arithmetic expression
	%_9 = icmp slt i32 %_8, 10

	;if statement
	br i1 %_9 ,label %if_0, label %else_0

if_0:

	;loading local variable
	%_10 = load i32, i32* %len

	;display an integer at stdout
	call void (i32) @print_int(i32 %_10)
	br label %fi_0

else_0:

	;display an integer at stdout
	call void (i32) @print_int(i32 5)
	br label %fi_0

fi_0:

	;loading local variable
	%_11 = load i32, i32* %len

	;apply arithmetic expression
	%_12 = icmp slt i32 0, %_11

	;if statement
	br i1 %_12 ,label %if_1, label %else_1

if_1:

	;loading local variable
	%_13 = load i32, i32* %len

	;display an integer at stdout
	call void (i32) @print_int(i32 %_13)
	br label %fi_1

else_1:

	;display an integer at stdout
	call void (i32) @print_int(i32 5)
	br label %fi_1

fi_1:
	ret i32 0
}

;Base.set
define i32 @Base.set (i8* %this, i32 %.x){
	;allocate space and store each parameter of the method
	%x = alloca i32
	store i32 %.x, i32* %x

	;load address of Base.data from memory
	%_14 = getelementptr i8, i8* %this, i32 10
	%_15 = bitcast i8* %_14 to i32*

	;store result
	store i32 %.x, i32* %_15
	ret i32 1
}

;Base.get
define i32 @Base.get (i8* %this){

	;load field Base.data from memory
	%_0 = getelementptr i8, i8* %this, i32 10
	%_1 = bitcast i8* %_0 to i32*
	%_2 = load i32, i32* %_1
	ret i32 %_2
}

;Derived.set
define i32 @Derived.set (i8* %this, i32 %.x){
	;allocate space and store each parameter of the method
	%x = alloca i32
	store i32 %.x, i32* %x
	ret i32 %.x
}

;Derived.myMethod
define i1 @Derived.myMethod (i8* %this){
	ret i1 0
}

