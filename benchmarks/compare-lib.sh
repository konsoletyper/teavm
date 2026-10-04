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

# Functions shared by compare-*.sh scripts. Not intended to be run directly.

BENCHMARKS_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(cd "$BENCHMARKS_DIR/.." && pwd)
BASELINE_DIR="$BENCHMARKS_DIR/build/compare/baseline"
REPORT_DIR="$BENCHMARKS_DIR/build/reports/teavm-perf"

# Runs benchmarks in project located at $1 and copies JSON results to $BASELINE_DIR.
# Remaining arguments are passed to Gradle.
run_baseline() {
    local root=$1
    shift
    local reports="$root/benchmarks/build/reports/teavm-perf"
    rm -f "$reports"/results-*.json
    (cd "$root" && ./gradlew :benchmarks:teavmBenchmark -Pbenchmark.formats=text,json "$@")

    rm -rf "$BASELINE_DIR"
    mkdir -p "$BASELINE_DIR"
    local found=0
    for file in "$reports"/results-*.json; do
        [ -e "$file" ] || continue
        cp "$file" "$BASELINE_DIR/"
        found=1
    done
    if [ "$found" != 1 ]; then
        echo "Baseline run did not produce any results" >&2
        return 1
    fi
}

# Runs benchmarks in the current project and compares results with ones collected by run_baseline.
# Results of baseline are labeled as baseline-<backend>, e.g. baseline-js.
# Arguments are passed to Gradle.
run_current() {
    local spec=""
    local file
    for file in "$BASELINE_DIR"/results-*.json; do
        local name
        name=$(basename "$file" .json)
        spec="$spec${spec:+,}baseline-${name#results-}=$file"
    done
    (cd "$ROOT_DIR" && ./gradlew :benchmarks:teavmBenchmark -Pbenchmark.baseline="$spec" "$@")
    echo
    echo "Comparison report: $REPORT_DIR/report.html"
}
