;for each class, declare a global vTable containing a pointer for each method
@.Base_vtable = global [2 x i8*] [i8* bitcast (i32 (i8*)* @Base.set to i8*), i8* bitcast (i1 (i8*, i32)* @Base.printInt to i8*)]

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

	;allocate space for a new "Base" object
	%_0 = call i8* @calloc(i32 1, i32 8)
	%_1 = bitcast i8* %_0 to i8***
	%_2 = getelementptr [2 x i8*], [2 x i8*]* @.Base_vtable, i32 0, i32 0
	store i8** %_2, i8*** %_1
	%_3 = bitcast i8* %_0 to i8*** 				;%_3 points to the vTable
	%_4 = load i8**, i8*** %_3				;%_4 is the vTable
	%_5 = getelementptr i8*, i8** %_4, i32 0	;%_5 points to the address of set
	%_6 = load i8*, i8** %_5					;%_6 points to the body of set
	%_7 = bitcast i8* %_6 to i32 (i8*)*	;%_cast pointer to the appropriate size
	%_8 = call i32 %_7(i8* %_0)

	;display an integer at stdout
	call void (i32) @print_int(i32 %_8)
	ret i32 0
}

;Base.set
define i32 @Base.set (i8* %this){
	;short-circuit and clause, right side gets evaluated if and only if left side evaluates to true
	br i1 0, label %true_0, label %false_0

true_0:
	
	%_9 = bitcast i8* %this to i8*** 				;%_9 points to the vTable
	%_10 = load i8**, i8*** %_9				;%_10 is the vTable
	%_11 = getelementptr i8*, i8** %_10, i32 1	;%_11 points to the address of printInt
	%_12 = load i8*, i8** %_11					;%_12 points to the body of printInt
	%_13 = bitcast i8* %_12 to i1 (i8*, i32)*	;%_cast pointer to the appropriate size
	%_14 = call i1 %_13(i8* %this, i32 99)
	br label %end_0

false_0:

	br label %end_0

end_0:

	%_15 = phi i1 [%_14, %true_0],[0, %false_0]

	;if statement
	br i1 %_15 ,label %if_0, label %else_0

if_0:

	;display an integer at stdout
	call void (i32) @print_int(i32 999)
	br label %fi_0

else_0:

	;display an integer at stdout
	call void (i32) @print_int(i32 21)
	br label %fi_0

fi_0:
	ret i32 0
}

;Base.printInt
define i1 @Base.printInt (i8* %this, i32 %.x){
	;allocate space and store each parameter of the method
	%x = alloca i32
	store i32 %.x, i32* %x

	;display an integer at stdout
	call void (i32) @print_int(i32 %.x)
	ret i1 1
}

