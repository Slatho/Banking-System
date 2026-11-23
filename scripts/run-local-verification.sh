#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$ROOT/target/core-compile"
find "$ROOT/src/main/java/com/bankingsystem/domain" \
     "$ROOT/src/main/java/com/bankingsystem/security" \
     "$ROOT/src/main/java/com/bankingsystem/persistence" \
     "$ROOT/src/main/java/com/bankingsystem/service" \
     "$ROOT/src/main/java/com/bankingsystem/controller" \
     -name '*.java' -print0 | xargs -0 javac --release 21 -d "$ROOT/target/core-compile"
echo "PASS: non-JavaFX core packages compile with Java 21"
"$ROOT/scripts/run-domain-smoke-tests.sh"
python3 "$ROOT/scripts/validate-sqlite-schema.py"
python3 "$ROOT/scripts/validate-database-migration.py"
echo "NOTE: run 'mvn clean test' and 'mvn javafx:run' in Codespaces or a local JavaFX desktop environment."
