#!/bin/bash

echo "Deleting old jars"
rm ./h1updatesite/artifacts.jar
rm ./h1updatesite/content.jar
rm ./h1updatesite/features/*
rm ./h1updatesite/plugins/* 

echo "############################"
echo "You now need to open ecl1 with eclipse (and installed PDE) and do the following steps:"
echo "1. Mark all projects and refresh them"
echo "2. Open ./h1updatesite/site.xml"
echo "3. Press the button \"Build All\""
echo "Please press enter when you have created the new jars"
echo "############################"
read 
