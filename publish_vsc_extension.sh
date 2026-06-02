#!/bin/bash

echo "=== Updating the VSCode extension jars -windows ==="
./update_vsc_extension_jars.sh windows

echo "=== Publishing target windows ==="
cd vscodeExtension/ecl1 && vsce publish --target win32-x64

echo "=== Updating the VSCode extension jars -linux ==="
cd ../..
./update_vsc_extension_jars.sh linux --skip-build

echo "=== Publishing target linux ==="
cd vscodeExtension/ecl1 && vsce publish --target linux-x64
