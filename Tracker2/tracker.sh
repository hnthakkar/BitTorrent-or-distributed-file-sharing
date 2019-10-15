#!/bin/bash
if [ -d "./bin" ]; then   
   rm -rd ./bin
fi
mkdir ./bin

echo "Compilation all java classes"
javac -d ./bin $(find ./src/* | grep .java)
echo "Successful"
java -cp "./bin" iiit.os.tracker.Tracker


