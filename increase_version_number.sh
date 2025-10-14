#!/bin/bash

# Uncomment me to set a manual version number
# NEW_VERSION=1.2.3

## Determine old version
OLD_VERSION=$(grep -Eo '[0-9]+\.[0-9]+\.[0-9]+\.[0-9]{4}-[0-9]{2}-[0-9]{2}' ./h1modulesfeature/feature.xml)
echo "Old Version: $OLD_VERSION"

## If no manual version number has been set: Compute new version number by increasing old version number and adding the current date
if [ -z "$NEW_VERSION" ]; then
    IFS='.' read -r MAJOR MINOR PATCH OLD_DATE <<< "$OLD_VERSION"
    NEW_PATCH=$(($PATCH + 1))
    NEW_DATE=$(date +%F)
    NEW_VERSION="$MAJOR.$MINOR.$NEW_PATCH.$NEW_DATE"
    echo "New version: $NEW_VERSION"
fi

## Update version number in feature.xml
echo "Updating version in ./h1modulesfeature/feature.xml"
sed -i --binary "s/$OLD_VERSION/$NEW_VERSION/" ./h1modulesfeature/feature.xml

## Update version number in MANIFEST.MF-Files of plugins
### Find all the META-INF/MANIFEST.MF files and store them in an array
FILES=$(find . -type f -path "*/META-INF/MANIFEST.MF")
for FILE in $FILES; do
    echo "Updating version in $FILE"
    # Use sed to find the current Bundle-Version and update it with the new version
    sed -i --binary "s/^Bundle-Version: .*/Bundle-Version: $NEW_VERSION/" "$FILE"
done

## Update version number on homepage
sed -i --binary "s/$OLD_VERSION/$NEW_VERSION/" ./net.sf.ecl1.website/index.html

## Update site.xml
sed -i --binary "s/$OLD_VERSION/$NEW_VERSION/g" ./h1updatesite/site.xml

## Update vscode extension package
echo "Updating version in ./vscodeExtension/ecl1/package.json"
sed -i --binary 's/"version": "[^"]*"/"version": "'$NEW_VERSION'"/' ./vscodeExtension/ecl1/package.json