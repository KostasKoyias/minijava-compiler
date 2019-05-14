#!/bin/bash

error_msg(){
    echo -e "\e[31m\e[1mError: \e[0mFailed to \e[1m$1\e[0m intermediate code $2 \e[1m$javaFile\e[0m"
    let fail++
}

execPath=$(dirname $(find .. -name "Main.class"))
cd $execPath
execPath=$(pwd)
testPath=$(find .. -name "in")
let fail=0
let shouldFail=1  # OutOfBounds should fail

# for each test case/file
for javaFile in $(ls $testPath)
do
    # generate intermediate code 
    timeout 3s java Main $testPath/$javaFile &> /dev/null
    if [ $? -ne 0 ]
    then
        error_msg "\e[95mgenerate" "for"
        continue 
    fi

    # compile to binary
    llFile="${javaFile%.java}.ll"
    timeout 3s clang-4.0 -o out $testPath/$llFile &> /dev/null 
    if [ $? -ne 0 ]
    then
        error_msg "\e[96mcompile" "of"
        continue 
    fi

    # execute
    timeout 3s ./out > out1  
    if [ $? -ne 0 ]
    then
        rm -f out*
        error_msg "\e[93mexecute" "of"
        continue 
    fi

    # compile and execute with javac and java
    javac $testPath/$javaFile
    cd $testPath
    exe="${javaFile%.java}"
    java $exe > out2

    # compare results
    diff $execPath/out1 out2 &> /dev/null
    let exitCode=$?
    rm -f out2
    cd $execPath
    if [ $exitCode -ne 0 ]
    then
        error_msg "\e[94mget same results\e[0m as javac" "for"
        continue 
    fi
    echo -e "\e[32m\e[1mNice: \e[0mGenerated, compiled and executed ir code for \e[1m$javaFile\e[0m same as javac"
done 

# cleanup and display end result
rm $testPath/*.ll out* $testPath/*class -f 
printf "\n\e[1mEnd result: "
notExpected=$(($fail-$shouldFail))
if [ $notExpected -ne 0 ]
then 
    echo -e "\e[31mFailure\e[0m (got unexpected results for $notExpected files)"
else 
    echo -e "\e[32mSuccess\e[0m"
fi
exit 0