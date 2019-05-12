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

	;while statement
	br label %while_0

while_0:

	;loading local variable
	%_8 = load i32, i32* %len

	;apply arithmetic expression
	%_9 = icmp slt i32 5, %_8
	br i1 %_9 ,label %do_0, label %done_0

do_0:

	;loading local variable
	%_10 = load i32, i32* %len

	;apply arithmetic expression
	%_11 = sub i32 %_10, 1

	;store result
	store i32 %_11, i32* %len

	;loading local variable
	%_12 = load i32, i32* %len

	;display an integer at stdout
	call void (i32) @print_int(i32 %_12)

	br label %while_0
done_0:

	ret i32 0
}

;Base.set
define i32 @Base.set (i8* %this, i32 %.x){
	;allocate space and store each parameter of the method
	%x = alloca i32
	store i32 %.x, i32* %x

	;load address of Base.data from memory
	%_13 = getelementptr i8, i8* %this, i32 10
	%_14 = bitcast i8* %_13 to i32*

	;store result
	store i32 %.x, i32* %_14
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

