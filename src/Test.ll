@.Base_vtable = global [2 x i8*] [i8* bitcast (i32 (i8*, i32)* @Base.set to i8*), i8* bitcast (i32 (i8*)* @Base.get to i8*)]
@.Derived_vtable = global [3 x i8*] [i8* bitcast (i32 (i8*, i32)* @Derived.set to i8*), i8* bitcast (i32 (i8*)* @Derived.get to i8*), i8* bitcast (i1 (i8*, i1, i8*)* @Derived.myMethod to i8*)]


declare i8* @calloc(i32, i32)
declare i32 @printf(i8*, ...)
declare void @exit(i32)

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
	call void (i32) @print_int(i32 23)
	ret i32 0
}
define i32 @Base.set (i8* %this, i32 %.x){
	%x = alloca i32
	store i32 %.x, i32* %x
	%_0 = load i32, i32* %x
	%_1 = getelementptr i8, i8* %this, i32 9
	%_2 = bitcast i8* %_1 to i32*
	store i32 %_0, i32* %_2
}

define i32 @Base.get (i8* %this){
}

define i32 @Derived.set (i8* %this, i32 %.x){
	%x = alloca i32
	store i32 %.x, i32* %x
	%_0 = getelementptr i8, i8* %this, i32 9
	%_1 = bitcast i8* %_0 to i32*
	%_2 = load i32, i32* %_1
	store i32 %_2, i32* %x
}

define i1 @Derived.myMethod (i8* %this, i1 %.y, i8* %.k){
	%y = alloca i1
	store i1 %.y, i1* %y
	%k = alloca i8*
	store i8* %.k, i8** %k
	%bl = alloca i1
	%_0 = load i1, i1* %y
	store i1 %_0, i1* %bl
}
