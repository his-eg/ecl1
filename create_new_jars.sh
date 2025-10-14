#!/bin/bash

echo "Deleting old jars"
rm ./h1updatesite/artifacts.jar
rm ./h1updatesite/content.jar
rm ./h1updatesite/features/*
rm ./h1updatesite/plugins/* 

echo "############################"
echo "You now need to open ecl1 with eclipse (and installed PDE) and do the following steps:"
echo "1. Mark all projects and refresh them"
echo "2. File -> Export -> Plug-in Development -> Deployable features"
echo "3. Choose the \"updatesite\" folder as the directoy"
echo "4. Export"
echo "Please press enter when you have created the new jars"
echo "############################"
read 
