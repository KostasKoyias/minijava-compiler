#!/bin/bash

error_msg(){
    echo -e "\e[31m\e[1mError: \e[0mFailed to \e[1m$1\e[0m intermediate code $2 $testPath/$i"
    let fail++
}

execPath=$(dirname $(find .. -name "Main.class"))
cd $execPath
testPath=$(find .. -name "tests")/in
let fail=0

# for each test case/file
for i in $(ls $testPath)
do
    # generate intermediate code 
    java Main $testPath/$i &> /dev/null
    if [ $? -ne 0 ]
    then
        error_msg "\e[95mgenerate" "for"
        continue 
    fi

    # compile to binary
    i="${i%.java}.ll"
    clang-4.0 -o out $testPath/$i &> /dev/null 
    if [ $? -ne 0 ]
    then
        error_msg "\e[96mcompile" "of"
        continue 
    fi

    # execute
    ./out &> /dev/null
    if [ $? -ne 0 ]
    then
        error_msg "\e[93mexecute" "of"
        continue 
    fi
    echo -e "\e[32m\e[1mNice: \e[0mGenerated, compiled and executed intermediate code for $testPath/${i%.ll}.java successfully"
done 

# cleanup and display end result
rm $testPath/*.ll
printf "\n\e[1mEnd result: "
if [ $fail -ne 0 ]
then 
    echo -e "\e[31mFailure ($fail files failed)\e[0m"
else 
    echo -e "\e[32mSuccess\e[0m"
fi
exit 0