#!/bin/bash

# Parse command line arguments
if [ $# -lt 1 ]; then
    echo "PIN for Yubikey missing. Exiting script."
    exit 1
fi

YUBIKEY_PIN=$1

# Add plugin jars
FILES=($(find -type f -path "./h1updatesite/plugins/*.jar"))
# Add feature jar
FILES+=($(find -type f -path "./h1updatesite/features/*.jar"))

for FILE in "${FILES[@]}"; do
    jarsigner -keystore NONE -storetype PKCS11 -storepass $YUBIKEY_PIN -providerClass sun.security.pkcs11.SunPKCS11 -providerArg ykcs11.conf $FILE "X.509 Certificate for PIV Authentication" -sigfile HIS_EG -tsa http://timestamp.digicert.com 
done