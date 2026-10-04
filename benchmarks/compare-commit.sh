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

# Compares current state of the project with given commit. Checks out the commit into a temporary
# git worktree, replaces benchmarks module there with the current one (so that both runs use the same
# benchmark code), runs benchmarks there to get baseline, then runs benchmarks in the current project
# and produces report that compares both runs. Current working tree is left untouched.
#
# Usage: benchmarks/compare-commit.sh <commit> [gradle arguments...]
# Example: benchmarks/compare-commit.sh HEAD~3 -Pbenchmark.backends=js -Pbenchmark.args="-wi 3 -i 5 String"

set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/compare-lib.sh"

if [ $# -lt 1 ]; then
    echo "Usage: $0 <commit> [gradle arguments...]" >&2
    exit 2
fi
COMMIT=$1
shift

cd "$ROOT_DIR"
COMMIT_ID=$(git rev-parse --verify "$COMMIT^{commit}")
WORKTREE=$(mktemp -d "${TMPDIR:-/tmp}/teavm-benchmark-XXXXXX")

cleanup() {
    git -C "$ROOT_DIR" worktree remove --force "$WORKTREE" >/dev/null 2>&1 || rm -rf "$WORKTREE"
    git -C "$ROOT_DIR" worktree prune
}
trap cleanup EXIT

echo "Checking out $COMMIT_ID into $WORKTREE"
git worktree add --detach "$WORKTREE" "$COMMIT_ID" >/dev/null

rm -rf "$WORKTREE/benchmarks"
tar -C "$ROOT_DIR" --exclude=benchmarks/build -cf - benchmarks | tar -C "$WORKTREE" -xf -
if ! grep -q '"benchmarks"' "$WORKTREE/settings.gradle.kts"; then
    echo "Commit $COMMIT_ID does not include benchmarks module into build, can't compare" >&2
    exit 1
fi

echo "Running baseline benchmarks at $COMMIT_ID"
run_baseline "$WORKTREE" "$@"

echo "Running benchmarks in current working tree"
run_current "$@"
