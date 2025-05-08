#!/bin/bash

# Check for correct number of arguments
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <new_version>"
    exit 1
fi

NEW_VERSION=$1

# Find all the META-INF/MANIFEST.MF files and store them in an array
FILES=$(find . -type f -path "*/META-INF/MANIFEST.MF")

echo "Updating all MANIFEST files..."

for FILE in $FILES; do
    echo "Updating version in $FILE"
    # Use sed to find the current Bundle-Version and update it with the new version
    sed -i "s/^Bundle-Version: .*/Bundle-Version: $NEW_VERSION/" "$FILE"
done

echo "Updating other files..."

# Update net.sf.ecl1.feature feature.xml 
echo "Updating version in ./h1modulesfeature/feature.xml"
sed -i '/label="ecl1 - HISinOne Extension Tools"/,/version="/s/version="[^"]*"/version="'$NEW_VERSION'"/' ./h1modulesfeature/feature.xml

# Update Website index.html
echo "Updating version in ./net.sf.ecl1.website/index.html"
sed -i 's/version of the ecl1 toolset is [^ ]*/version of the ecl1 toolset is '$NEW_VERSION'/' ./net.sf.ecl1.website/index.html

# Update vscode extension package
echo "Updating version in ./vscodeExtension/ecl1/package.json"
sed -i 's/"version": "[^"]*"/"version": "'$NEW_VERSION'"/' ./vscodeExtension/ecl1/package.json

echo "Version update completed."
