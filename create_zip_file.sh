#!/bin/bash

cd ./h1updatesite

# Delete old zip file
rm ./*.zip

# Create new zip-file
CURRENT_DATE=$(date +%F)
tar -c -a -f ./updatesite-$CURRENT_DATE.zip index.html artifacts.jar content.jar site.xml features/* plugins/* web/*


cd ..