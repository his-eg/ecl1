###############################################################################
#									      
# Release script for ecl1
#
# Prerequisites: 
# - Prebuilt Update Site
# - build.ant-private.properties
# - README.md up to date
# - No uncommitted changes
#
# Parameters: 1. Version identificator (required)                          
#									      
###############################################################################
VERSION=$1
###############################################################################

# Update site
# Sign update site artifacts
cd h1updatesite
ant sign
git commit -am "Signed update site artifacts for version ${VERSION}"

cd ..

# Website
# Update version number
cd net.sf.ecl1.website
cat index.html.template | sed "s|ECL1VERSION|$VERSION|" > index.html.tmp
mv index.html.tmp index.html
cd ..

# Tag version
git tag -a v$VERSION -m "Release tag for version ${VERSION}"

# Website
# Upload website
cd net.sf.ecl1.website
./update.sh
cd ..

# push tags to sf.net
git push --tags
git push

# upload update site and update site archive
cd h1updatesite
ant upload
cd ..
