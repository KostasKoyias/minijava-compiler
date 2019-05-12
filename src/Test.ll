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

	;allocate space for local variable %b
	%b = alloca i8*

	;allocate space for local variable %d
	%d = alloca i8*

	;allocate space for a new "Derived" object
	%_0 = call i8* @calloc(i32 1, i32 22)
	%_1 = bitcast i8* %_0 to i8***
	%_2 = getelementptr [3 x i8*], [3 x i8*]* @.Derived_vtable, i32 0, i32 0
	store i8** %_2, i8*** %_1

	;store result
	store i8* %_0, i8** %d
	ret i32 0
}

;Base.set
define i32 @Base.set (i8* %this, i32 %.x){
	;allocate space and store each parameter of the method
	%x = alloca i32
	store i32 %.x, i32* %x

	;load address of Base.data from memory
	%_3 = getelementptr i8, i8* %this, i32 10
	%_4 = bitcast i8* %_3 to i32*

	;store result
	store i32 %.x, i32* %_4

	;loading local variable
	%_5 = load i32, i32* %_4
	ret i32 %_5
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

	;apply arithmetic expression
	%_0 = mul i32 %.x, 2

	;load address of Derived.data from memory
	%_1 = getelementptr i8, i8* %this, i32 10
	%_2 = bitcast i8* %_1 to i32*

	;store result
	store i32 %_0, i32* %_2

	;loading local variable
	%_3 = load i32, i32* %_2
	ret i32 %_3
}

;Derived.myMethod
define i1 @Derived.myMethod (i8* %this){
	ret i1 0
}

