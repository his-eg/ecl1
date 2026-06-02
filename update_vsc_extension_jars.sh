#!/usr/bin/env bash
set -euo pipefail

VSCODE_LIB="./vscodeExtension/ecl1/jars"
TARGET="${1:-}"
SKIP_BUILD="${2:-}"

# Check target
case "$TARGET" in
  windows) SUFFIX="win32-x64" ;;
  linux)   SUFFIX="linux-x64" ;;
  *)
    echo "Usage: $0 <windows|linux> [--skip-build]"
    exit 1
    ;;
esac

# Build new jars (optional skip)
if [[ "$SKIP_BUILD" != "--skip-build" ]]; then
  echo "Building jars..."
  ./gradlew clean build
fi

rm -rf "$VSCODE_LIB"
mkdir -p "$VSCODE_LIB"

# Copy and rename platform-jars to vsc-extension jars-folder
COUNT=0
while IFS= read -r -d '' FILE; do
  NAME="$(basename "$FILE")"
  TARGET_NAME="${NAME%-"$SUFFIX".jar}-all.jar"

  echo "Copying $NAME"
  cp "$FILE" "$VSCODE_LIB/$TARGET_NAME"

  COUNT=$((COUNT + 1))
done < <(
  find . -type f -path "*/build/libs/*-${SUFFIX}.jar" \
    ! -name "net.sf.abc.updatecheck-${SUFFIX}.jar" -print0
)

echo "Copied $COUNT jars for $TARGET."
