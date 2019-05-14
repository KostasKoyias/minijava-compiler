;for each class, declare a global vTable containing a pointer for each method
@.Base_vtable = global [2 x i8*] [i8* bitcast (i32 (i8*)* @Base.init to i8*), i8* bitcast (i32 (i8*)* @Base.get to i8*)]

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
	%_0 = call i8* @calloc(i32 1, i32 16)
	%_1 = bitcast i8* %_0 to i8***
	%_2 = getelementptr [2 x i8*], [2 x i8*]* @.Base_vtable, i32 0, i32 0
	store i8** %_2, i8*** %_1

	;store result
	store i8* %_0, i8** %b

	;loading local variable 'b' from stack
	%_3 = load i8*, i8** %b
	%_4 = bitcast i8* %_3 to i8*** 				;%_4 points to the vTable
	%_5 = load i8**, i8*** %_4				;%_5 is the vTable
	%_6 = getelementptr i8*, i8** %_5, i32 0	;%_6 points to the address of init
	%_7 = load i8*, i8** %_6					;%_7 points to the body of init
	%_8 = bitcast i8* %_7 to i32 (i8*)*	;%_cast pointer to the appropriate size
	%_9 = call i32 %_8(i8* %_3)

	;display an integer at stdout
	call void (i32) @print_int(i32 %_9)
	%_10 = bitcast i8* %_3 to i8*** 				;%_10 points to the vTable
	%_11 = load i8**, i8*** %_10				;%_11 is the vTable
	%_12 = getelementptr i8*, i8** %_11, i32 1	;%_12 points to the address of get
	%_13 = load i8*, i8** %_12					;%_13 points to the body of get
	%_14 = bitcast i8* %_13 to i32 (i8*)*	;%_cast pointer to the appropriate size
	%_15 = call i32 %_14(i8* %_3)

	;display an integer at stdout
	call void (i32) @print_int(i32 %_15)
	ret i32 0
}

;Base.init
define i32 @Base.init (i8* %this){

	;allocate space for new array of size 10 + 1 place to store size at
	%_16 = add i32 10, 1
	%_17 = call i8* @calloc(i32 4, i32 %_16)
	%_18 = bitcast i8* %_17 to i32*

	;store size at index 0
	store i32 10, i32* %_18

	;load address of Base.num from memory
	%_19 = getelementptr i8, i8* %this, i32 8
	%_20 = bitcast i8* %_19 to i8**

	;adjust pointer type of left operand
	%_21 = bitcast i8** %_20 to i32**

	;store result
	store i32* %_18, i32** %_21

	;load array
	%_22 = load i8*, i8** %_20

	;get length of array at %_22
	%_23 = bitcast i8* %_22 to i32*
	%_24 = getelementptr i32, i32* %_23, i32 0
	%_25 = load i32, i32* %_24

	;make sure index "4" is within bounds
	%_26 = icmp slt i32 4, 0
	%_27 = icmp slt i32 4, %_25
	%_28 = xor i1 %_26, %_27
	br i1 %_28, label %withinBounds_0, label %outOfBounds_0

outOfBounds_0:

	call void @throw_oob()
	br label %withinBounds_0

withinBounds_0:

	;assign a value to the array element
	%_29 = load i8*, i8** %_20
	%_30 = bitcast i8* %_29 to i32*
	%_31 = getelementptr i32, i32* %_30 , i32 5
	store i32 3012, i32* %_31
	ret i32 0
}

;Base.get
define i32 @Base.get (i8* %this){

	;load field Base.num from memory
	%_0 = getelementptr i8, i8* %this, i32 8
	%_1 = bitcast i8* %_0 to i8**
	%_2 = load i8*, i8** %_1

	;get length of array at %_2
	%_3 = bitcast i8* %_2 to i32*
	%_4 = getelementptr i32, i32* %_3, i32 0
	%_5 = load i32, i32* %_4

	;make sure index "4" is within bounds
	%_6 = icmp slt i32 4, 0
	%_7 = icmp slt i32 4, %_5
	%_8 = xor i1 %_6, %_7
	br i1 %_8, label %withinBounds_0, label %outOfBounds_0

outOfBounds_0:

	call void @throw_oob()
	br label %withinBounds_0

withinBounds_0:

	;lookup *(%_2 + 5)
	%_9 = bitcast i8* %_2 to i32*
	%_10 = getelementptr i32, i32* %_9, i32 5
	%_11 = load i32, i32* %_10
	ret i32 %_11
}

