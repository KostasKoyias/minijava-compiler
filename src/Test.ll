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

	;allocate space for local variable %x
	%x = alloca i32

	;allocate space for new array of size 4 + 1 place to store size at
	%_0 = add i32 4, 1
	%_1 = call i8* @calloc(i32 1, i32 %_0)
	%_2 = bitcast i8* %_1 to i32*

	;store size at index 0
	store i32 4, i32* %_2

	;adjust pointer type of left operand
	%_3 = bitcast i8** %r to i32**

	;store result
	store i32* %_2, i32** %_3

	;load array
	%_4 = load i8*, i8** %r

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
	%_11 = load i8*, i8** %r
	%_12 = bitcast i8* %_11 to i32*
	%_13 = getelementptr i32, i32* %_12 , i32 3
	store i32 19, i32* %_13

	;get length of array at %_11
	%_14 = bitcast i8* %_11 to i32*
	%_15 = getelementptr i32, i32* %_14, i32 0
	%_16 = load i32, i32* %_15

	;make sure index "2" is within bounds
	%_17 = icmp slt i32 2, 0
	%_18 = icmp slt i32 2, %_16
	%_19 = xor i1 %_17, %_18
	br i1 %_19, label %withinBounds_1, label %outOfBounds_1

outOfBounds_1:

	call void @throw_oob()
	br label %withinBounds_1

withinBounds_1:


	;lookup *(%_11 + 3)
	%_20 = bitcast i8* %_11 to i32*
	%_21 = getelementptr i32, i32* %_20, i32 3
	%_22 = load i32, i32* %_21

	;store result
	store i32 %_22, i32* %x

	;loading local variable
	%_23 = load i32, i32* %x

	;display an integer at stdout
	call void (i32) @print_int(i32 %_23)
	ret i32 0
}

;Base.set
define i32 @Base.set (i8* %this, i32 %.x){
	;allocate space and store each parameter of the method
	%x = alloca i32
	store i32 %.x, i32* %x

	;load address of Base.data from memory
	%_24 = getelementptr i8, i8* %this, i32 10
	%_25 = bitcast i8* %_24 to i32*

	;store result
	store i32 %.x, i32* %_25

	;loading local variable
	%_26 = load i32, i32* %_25
	ret i32 %_26
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

