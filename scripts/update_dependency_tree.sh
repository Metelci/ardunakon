#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
out_file="${repo_root}/dependency_tree.txt"

echo "Generating dependency tree..."
"${repo_root}/gradlew" :app:dependencies > "${out_file}"
echo "Wrote ${out_file}"
