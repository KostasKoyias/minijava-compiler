;for each class, declare a global vTable containing a pointer for each method
@.Base_vtable = global [2 x i8*] [i8* bitcast (i32 (i8*, i32)* @Base.set to i8*), i8* bitcast (i32 (i8*)* @Base.get to i8*)]
@.Derived_vtable = global [2 x i8*] [i8* bitcast (i32 (i8*, i32)* @Derived.set to i8*), i8* bitcast (i32 (i8*)* @Base.get to i8*)]

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
	%_0 = call i8* @calloc(i32 1, i32 29)
	%_1 = bitcast i8* %_0 to i8***
	%_2 = getelementptr [2 x i8*], [2 x i8*]* @.Derived_vtable, i32 0, i32 0
	store i8** %_2, i8*** %_1

	;store result
	store i8* %_0, i8** %d

	;loading local variable
	%_3 = load i8*, i8** %d

	;store result
	store i8* %_3, i8** %b

	;loading local variable
	%_4 = load i8*, i8** %b

	;allocate space for a new "Base" object
	%_5 = call i8* @calloc(i32 1, i32 21)
	%_6 = bitcast i8* %_5 to i8***
	%_7 = getelementptr [2 x i8*], [2 x i8*]* @.Base_vtable, i32 0, i32 0
	store i8** %_7, i8*** %_6
	%_8 = bitcast i8* %_5 to i8*** 				;%_8 points to the vTable
	%_9 = load i8**, i8*** %_8				;%_9 is the vTable
	%_10 = getelementptr i8*, i8** %_9, i32 1	;%_10 points to the address of get
	%_11 = load i8*, i8** %_10					;%_11 points to the body of get
	%_12 = bitcast i8* %_11 to i32 (i8*)*	;%_cast pointer to the appropriate size
	%_13 = call i32 %_12(i8* %_5)
	%_14 = bitcast i8* %_4 to i8*** 				;%_14 points to the vTable
	%_15 = load i8**, i8*** %_14				;%_15 is the vTable
	%_16 = getelementptr i8*, i8** %_15, i32 0	;%_16 points to the address of set
	%_17 = load i8*, i8** %_16					;%_17 points to the body of set
	%_18 = bitcast i8* %_17 to i32 (i8*, i32)*	;%_cast pointer to the appropriate size
	%_19 = call i32 %_18(i8* %_4, i32 %_13)

	;display an integer at stdout
	call void (i32) @print_int(i32 %_19)
	ret i32 0
}

;Base.set
define i32 @Base.set (i8* %this, i32 %.x){
	;allocate space and store each parameter of the method
	%x = alloca i32
	store i32 %.x, i32* %x
	ret i32 0
}

;Base.get
define i32 @Base.get (i8* %this){

	;load field Base.data from memory
	%_0 = getelementptr i8, i8* %this, i32 17
	%_1 = bitcast i8* %_0 to i32*
	%_2 = load i32, i32* %_1

	;apply arithmetic expression
	%_3 = add i32 %_2, 30
	ret i32 %_3
}

;Derived.set
define i32 @Derived.set (i8* %this, i32 %.x){
	;allocate space and store each parameter of the method
	%x = alloca i32
	store i32 %.x, i32* %x

	;allocate space for local variable %i
	%i = alloca i32

	;store result
	store i32 1, i32* %i

	;while statement
	br label %while_0

while_0:

	;loading local variable
	%_0 = load i32, i32* %i

	;apply arithmetic expression
	%_1 = icmp slt i32 %_0, 4
	br i1 %_1 ,label %do_0, label %done_0

do_0:

	;loading local variable
	%_2 = load i32, i32* %i

	;display an integer at stdout
	call void (i32) @print_int(i32 %_2)

	;loading local variable
	%_3 = load i32, i32* %i

	;apply arithmetic expression
	%_4 = add i32 %_3, 1

	;store result
	store i32 %_4, i32* %i

	;apply arithmetic expression
	%_5 = sub i32 %.x, 1

	;store result
	store i32 %_5, i32* %x

	br label %while_0
done_0:


	;load address of Derived.data from memory
	%_6 = getelementptr i8, i8* %this, i32 17
	%_7 = bitcast i8* %_6 to i32*

	;store result
	store i32 %.x, i32* %_7

	;loading local variable
	%_8 = load i32, i32* %_7
	ret i32 %_8
}

