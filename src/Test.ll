;for each class, declare a global vTable containing a pointer for each method
@.Base_vtable = global [1 x i8*] [i8* bitcast (i32 (i8*, i32)* @Base.set to i8*)]

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

	;allocate space for a new "Base" object
	%_0 = call i8* @calloc(i32 1, i32 12)
	%_1 = bitcast i8* %_0 to i8***
	%_2 = getelementptr [1 x i8*], [1 x i8*]* @.Base_vtable, i32 0, i32 0
	store i8** %_2, i8*** %_1

	;store result
	store i8* %_0, i8** %b

	;loading local variable 'b' from stack
	%_3 = load i8*, i8** %b
	%_4 = bitcast i8* %_3 to i8*** 				;%_4 points to the vTable
	%_5 = load i8**, i8*** %_4				;%_5 is the vTable
	%_6 = getelementptr i8*, i8** %_5, i32 0	;%_6 points to the address of set
	%_7 = load i8*, i8** %_6					;%_7 points to the body of set
	%_8 = bitcast i8* %_7 to i32 (i8*, i32)*	;%_cast pointer to the appropriate size
	%_9 = call i32 %_8(i8* %_3, i32 30)

	;display an integer at stdout
	call void (i32) @print_int(i32 %_9)
	ret i32 0
}

;Base.set
define i32 @Base.set (i8* %this, i32 %.x){
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

	;loading local variable 'i' from stack
	%_10 = load i32, i32* %i

	;apply arithmetic expression
	%_11 = icmp slt i32 %_10, 4
	br i1 %_11 ,label %do_0, label %done_0

do_0:

	;loading local variable 'i' from stack
	%_12 = load i32, i32* %i

	;display an integer at stdout
	call void (i32) @print_int(i32 %_12)

	;loading local variable 'i' from stack
	%_13 = load i32, i32* %i

	;apply arithmetic expression
	%_14 = add i32 %_13, 1

	;store result
	store i32 %_14, i32* %i

	;loading local variable 'x' from stack
	%_15 = load i32, i32* %x

	;apply arithmetic expression
	%_16 = sub i32 %_15, 1

	;store result
	store i32 %_16, i32* %x

	br label %while_0
done_0:


	;loading local variable 'x' from stack
	%_17 = load i32, i32* %x
	ret i32 %_17
}

