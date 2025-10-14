#!/bin/bash

###############################################################################
# Parameters:
# 1. Yubikey Pin (mandatory)
###############################################################################

# Parse command line arguments
if [ $# -lt 1 ]; then
    echo "PIN for Yubikey missing. Exiting script. Usage:"
    echo "$0 <YUBIKEY_PIN>"
    exit 1
fi

YUBIKEY_PIN=$1

# Increase version number
source ./increase_version_number.sh

# Delete old jars and create new jars
./create_new_jars.sh

# Sign new jars
./sign_jars.sh $YUBIKEY_PIN

# Create zip file
./create_zip_file.sh

# Create git commit and git tag
./git_commit.sh $NEW_VERSION

# Inform the user what manual steps still need to be done:
echo "############################"
echo "Script completed. The following manual steps are needed:"
echo "1. Please manually add an entry for the new version in the following file: ./h1updatesite/README.md"
echo "2. Push your changes: "
echo "2.1 git push #push commits"
echo "2.2 git push --tags #push new tag"
echo "3. Update the update sites"
echo "3.1 https://devtools.his.de/ecl1/"
echo "3.2 https://sourceforge.net/projects/ecl1/files/"
