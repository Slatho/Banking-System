#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/target/domain-smoke"
mkdir -p "$OUT"
find "$ROOT/src/main/java/com/bankingsystem/domain" "$ROOT/src/main/java/com/bankingsystem/security" -name '*.java' -print0 | xargs -0 javac --release 21 -d "$OUT"
javac --release 21 -cp "$OUT" -d "$OUT" "$ROOT/scripts/DomainSmokeTest.java"
java -cp "$OUT" DomainSmokeTest
