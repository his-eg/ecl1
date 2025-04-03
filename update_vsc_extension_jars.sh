#!/bin/bash

VSCODE_LIB="./vscodeExtension/ecl1/jars"

echo "Building jars.."

./gradlew clean build

# Delete all old files in lib
rm -rf "$VSCODE_LIB"/*

# Find all Ecl1 JAR files, excluding updatecheck, since VSCode handles updates automatically
FILES=$(find . -type f -path "*/build/libs/*-all.jar"  ! -name "net.sf.ecl1.updatecheck-all.jar")

for FILE in $FILES; do
    echo "Copying $FILE to $VSCODE_LIB"
    cp "$FILE" "$VSCODE_LIB/"
done

echo "Done!"
