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

	;allocate space for new array of size 2 + 1 place to store size at
	%_0 = add i32 2, 1
	%_1 = call i8* @calloc(i32 1, i32 %_0)
	%_2 = bitcast i8* %_1 to i32*

	;store size at index 0
	store i32 2, i32* %_2

	;adjust pointer type of left operand
	%_3 = bitcast i8** %r to i32**

	;store result
	store i32* %_2, i32** %_3

	;assign a value to the array element
	%_4 = load i8*, i8** %r
	%_5 = bitcast i8* %_4 to i32*
	%_6 = getelementptr i32, i32* %_5 , i32 2
	store i32 108, i32* %_6

	;lookup *(%_4 + 1)
	%_7 = bitcast i8* %_4 to i32*
	%_8 = getelementptr i32, i32* %_7, i32 2
	%_9 = load i32, i32* %_8
	call void (i32) @print_int(i32 %_9)
	call void (i32) @print_int(i32 23)
	ret i32 0
}

;Base.set
define i32 @Base.set (i8* %this, i32 %.x){
	;allocate space and store each parameter of the method
	%x = alloca i32
	store i32 %.x, i32* %x

	;load address of Base.data from memory
	%_10 = getelementptr i8, i8* %this, i32 10
	%_11 = bitcast i8* %_10 to i32*

	;store result
	store i32 %.x, i32* %_11
	call void (i32) @print_int(i32 %.x)

	;loading local variable
	%_12 = load i32, i32* %_11
	ret i32 %_12
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

