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

	;allocate space for local variable %x
	%x = alloca i8*

	;allocate space for new array of size 12 + 1 place to store size at
	%_0 = add i32 12, 1
	%_1 = call i8* @calloc(i32 4, i32 %_0)
	%_2 = bitcast i8* %_1 to i32*

	;store size at index 0
	store i32 12, i32* %_2

	;adjust pointer type of left operand
	%_3 = bitcast i8** %x to i32**

	;store result
	store i32* %_2, i32** %_3

	;load array
	%_4 = load i8*, i8** %x

	;get length of array at %_4
	%_5 = bitcast i8* %_4 to i32*
	%_6 = getelementptr i32, i32* %_5, i32 0
	%_7 = load i32, i32* %_6

	;make sure index "2" is within bounds
	%_8 = icmp slt i32 2, 0
	%_9 = icmp slt i32 2, %_7
	%_10 = xor i1 %_8, %_9
	br i1 %_10, label %withinBounds_0, label %outOfBounds_0

outOfBounds_0:

	call void @throw_oob()
	br label %withinBounds_0

withinBounds_0:


	;assign a value to the array element
	%_11 = load i8*, i8** %x
	%_12 = bitcast i8* %_11 to i32*
	%_13 = getelementptr i32, i32* %_12 , i32 3
	store i32 5, i32* %_13

	;loading local variable
	%_14 = load i8*, i8** %x

	;get length of array at %_14
	%_15 = bitcast i8* %_14 to i32*
	%_16 = getelementptr i32, i32* %_15, i32 0
	%_17 = load i32, i32* %_16

	;make sure index "11" is within bounds
	%_18 = icmp slt i32 11, 0
	%_19 = icmp slt i32 11, %_17
	%_20 = xor i1 %_18, %_19
	br i1 %_20, label %withinBounds_1, label %outOfBounds_1

outOfBounds_1:

	call void @throw_oob()
	br label %withinBounds_1

withinBounds_1:


	;lookup *(%_14 + 12)
	%_21 = bitcast i8* %_14 to i32*
	%_22 = getelementptr i32, i32* %_21, i32 12
	%_23 = load i32, i32* %_22

	;display an integer at stdout
	call void (i32) @print_int(i32 %_23)

	;allocate space for a new "Derived" object
	%_24 = call i8* @calloc(i32 1, i32 22)
	%_25 = bitcast i8* %_24 to i8***
	%_26 = getelementptr [3 x i8*], [3 x i8*]* @.Derived_vtable, i32 0, i32 0
	store i8** %_26, i8*** %_25

	;store result
	store i8* %_24, i8** %d

	;loading local variable
	%_27 = load i8*, i8** %d

	;store result
	store i8* %_27, i8** %b

	;loading local variable
	%_28 = load i8*, i8** %b
	%_29 = bitcast i8* %_28 to i8*** 				;29 points to the vTable
	%_30 = load i8**, i8*** %_29					;30 is the vTable
	%_31 = getelementptr i8*, i8** %_30, i32 0	;31 points to the address of set
	%_32 = load i8*, i8** %_31					;32 point to the body of set
	%_33 = bitcast i8* %_32 to i32 (i8*, i32)*	;cast pointer to the appropriate size
	%_34 = call i32 %_33(i8* %_28, i32 10)

	;display an integer at stdout
	call void (i32) @print_int(i32 %_34)
	ret i32 0
}

;Base.set
define i32 @Base.set (i8* %this, i32 %.x){
	;allocate space and store each parameter of the method
	%x = alloca i32
	store i32 %.x, i32* %x

	;load address of Base.data from memory
	%_35 = getelementptr i8, i8* %this, i32 10
	%_36 = bitcast i8* %_35 to i32*

	;store result
	store i32 %.x, i32* %_36

	;loading local variable
	%_37 = load i32, i32* %_36
	ret i32 %_37
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

