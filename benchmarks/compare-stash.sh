#!/usr/bin/env bash
#
#  Copyright 2026 Alexey Andreev.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

# Measures effect of uncommitted changes. Stashes all changes (including untracked files) except for ones
# in the benchmarks module, runs benchmarks to get baseline, restores changes, runs benchmarks again
# and produces report that compares both runs.
#
# Usage: benchmarks/compare-stash.sh [gradle arguments...]
# Example: benchmarks/compare-stash.sh -Pbenchmark.backends=js,wasm-gc -Pbenchmark.args="-wi 3 -i 5 String"

set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/compare-lib.sh"

cd "$ROOT_DIR"
STASH_MESSAGE="teavm-benchmarks-compare-$$"
is_stashed() {
    [[ "$(git stash list -n 1 --format=%s)" == *"$STASH_MESSAGE" ]]
}

git stash push --include-untracked -m "$STASH_MESSAGE" -- . ':(exclude)benchmarks' >/dev/null
if ! is_stashed; then
    echo "No changes outside of benchmarks module, nothing to compare" >&2
    exit 1
fi

restore() {
    if is_stashed; then
        echo "Restoring stashed changes"
        git stash pop --index >/dev/null || git stash pop >/dev/null
    fi
}
trap restore EXIT

echo "Running baseline benchmarks (without uncommitted changes)"
run_baseline "$ROOT_DIR" "$@"

restore
trap - EXIT

echo "Running benchmarks with uncommitted changes"
run_current "$@"
