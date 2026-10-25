#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
task_java_home="${JAVA_HOME:-$HOME/dev-tools/java-22}"
mkdir -p build/tests
"$task_java_home/bin/javac" --release 8 -Xlint:-options -d build/tests common/src/main/java/com/nuttyvr/common/ModRules.java tests/ModRulesTest.java
"$task_java_home/bin/java" -cp build/tests ModRulesTest
python3 tests/check_apks.py
if [ -f prebuilt/nightly/build.json ]; then
    python3 tests/test_nightly.py
fi
