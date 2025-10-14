#!/bin/bash

# Parse command line arguments
if [ $# -lt 1 ]; then
    echo "Version number missing. Will not commit changes."
    exit 1
fi

NEW_VERSION=$1

# Ask user
read -p "Do you really want to commit? [y/N] " answer

# Convert answer to lowercase to make it case-insensitive
answer="${answer,,}"

if [[ "$answer" == "n" || "$answer" == "no" ]]; then
    echo "Aborting git commands"
    exit 1
fi

git add .
git commit -m "release $NEW_VERSION"
git tag -a "v$NEW_VERSION" -m "Release tag for version ${NEW_VERSION}"