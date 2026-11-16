#!/usr/bin/env bash
# Use the same Java formatter as Android Studio, without downloading another tool.
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
studio_dir=${TABKHA_STUDIO_HOME:-"$HOME/android-studio"}
if [[ ! -x "$studio_dir/bin/format.sh" ]]; then
    echo 'Set TABKHA_STUDIO_HOME to the Android Studio installation folder.' >&2
    exit 1
fi
exec "$studio_dir/bin/format.sh" -s "$project_dir/tools/java-code-style.xml" -r -m '*.java' \
    "$project_dir/app/src" "$project_dir/recipe-core/src" "$project_dir/backend/src"
