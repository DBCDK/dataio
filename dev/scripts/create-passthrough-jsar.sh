#!/usr/bin/env bash
# Creates two passthrough JSAR files in dev/testdata/:
#   passthrough-nashorn.jsar  – for job-processor2 (Nashorn engine)
#   passthrough-graaljs.jsar  – for job-processor-graaljs (GraalJS engine)
#
# A JSAR is a ZIP file containing JavaScript source files and a META-INF/MANIFEST.MF
# that declares the entrypoint script and function.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TESTDATA_DIR="$SCRIPT_DIR/../testdata"
WORK_DIR=$(mktemp -d)
trap 'rm -rf "$WORK_DIR"' EXIT

mkdir -p "$WORK_DIR/META-INF"
cp "$TESTDATA_DIR/passthrough.js" "$WORK_DIR/passthrough.js"

build_jsar() {
    local outfile="$1"
    local flow_name="$2"
    local engine_line="$3"
    cat > "$WORK_DIR/META-INF/MANIFEST.MF" <<EOF
Manifest-Version: 1.0
Flow-Name: ${flow_name}
Flow-Description: Returns input record unchanged
Flow-Entrypoint-Script: passthrough.js
Flow-Entrypoint-Function: process
${engine_line}
EOF
    # Trailing newline required by the Manifest spec
    echo "" >> "$WORK_DIR/META-INF/MANIFEST.MF"

    rm -f "$outfile"
    (cd "$WORK_DIR" && zip -q "$outfile" META-INF/MANIFEST.MF passthrough.js)
    echo "Created $(basename "$outfile")"
}

build_jsar "$TESTDATA_DIR/passthrough-nashorn.jsar" "Passthrough" ""
build_jsar "$TESTDATA_DIR/passthrough-graaljs.jsar" "Passthrough GraalJS" "Flow-JavaScript-Engine: GRAALJS"
