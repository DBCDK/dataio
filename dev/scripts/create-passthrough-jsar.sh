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

build_jsar() {
    local src_dir="$1"
    local outfile="$2"
    local flow_name="$3"
    local engine_line="$4"

    local work_dir
    work_dir=$(mktemp -d)
    trap 'rm -rf "$work_dir"' RETURN

    cp -r "$src_dir/." "$work_dir/"
    mkdir -p "$work_dir/META-INF"
    {
        echo "Manifest-Version: 1.0"
        echo "Flow-Name: ${flow_name}"
        echo "Flow-Description: Returns input record unchanged"
        echo "Flow-Entrypoint-Script: passthrough.js"
        echo "Flow-Entrypoint-Function: process"
        [[ -n "$engine_line" ]] && echo "$engine_line"
        echo ""
    } > "$work_dir/META-INF/MANIFEST.MF"

    rm -f "$outfile"
    (cd "$work_dir" && zip -qr "$outfile" .)
    echo "Created $(basename "$outfile")"
}

build_jsar "$SCRIPT_DIR/jsar-nashorn" "$TESTDATA_DIR/passthrough-nashorn.jsar" "Passthrough" ""
build_jsar "$SCRIPT_DIR/jsar-graaljs" "$TESTDATA_DIR/passthrough-graaljs.jsar" "Passthrough GraalJS" "Flow-JavaScript-Engine: GRAALJS"
