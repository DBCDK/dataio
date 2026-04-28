#!/usr/bin/env bash
# Seeds the flow-store with the two passthrough flows, a submitter, a sink, and two flow binders.
# Safe to run multiple times: existing entities are detected and reused rather than duplicated.
set -euo pipefail

FLOWSTORE="http://localhost:8081/dataio/flow-store-service"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TESTDATA_DIR="$SCRIPT_DIR/../testdata"

die() { echo "ERROR: $*" >&2; exit 1; }
require_jq() { command -v jq >/dev/null 2>&1 || die "jq is required (apt install jq / brew install jq)"; }
require_jq

# ── flows ──────────────────────────────────────────────────────────────────────

upload_or_find_flow() {
    local jsar="$1" expected_name="$2"

    # Check if a flow with this name already exists
    local existing
    existing=$(curl -sf "$FLOWSTORE/flows" | jq -r --arg name "$expected_name" '.[] | select(.content.name == $name) | .id' 2>/dev/null || true)
    if [[ -n "$existing" ]]; then
        echo "$existing"
        return
    fi

    local response
    response=$(curl -sf -X POST \
        "$FLOWSTORE/flows/jsar/$(date +%s%3N)" \
        -H "Content-Type: application/octet-stream" \
        --data-binary "@$jsar")
    echo "$response" | jq -r '.id'
}

echo "Uploading Nashorn passthrough flow..."
NASHORN_FLOW_ID=$(upload_or_find_flow "$TESTDATA_DIR/passthrough-nashorn.jsar" "Passthrough")
[[ "$NASHORN_FLOW_ID" =~ ^[0-9]+$ ]] || die "Failed to upload Nashorn flow (got: $NASHORN_FLOW_ID)"
echo "  Nashorn flow ID: $NASHORN_FLOW_ID"

echo "Uploading GraalJS passthrough flow..."
GRAALJS_FLOW_ID=$(upload_or_find_flow "$TESTDATA_DIR/passthrough-graaljs.jsar" "Passthrough GraalJS")
[[ "$GRAALJS_FLOW_ID" =~ ^[0-9]+$ ]] || die "Failed to upload GraalJS flow (got: $GRAALJS_FLOW_ID)"
echo "  GraalJS flow ID: $GRAALJS_FLOW_ID"

# ── submitter ──────────────────────────────────────────────────────────────────

echo "Creating submitter..."
existing_submitter=$(curl -sf "$FLOWSTORE/submitters" | jq -r '.[] | select(.content.number == 870970) | .id' 2>/dev/null || true)
if [[ -n "$existing_submitter" ]]; then
    SUBMITTER_ID="$existing_submitter"
    echo "  Submitter already exists, ID: $SUBMITTER_ID"
else
    submitter_response=$(curl -sf -X POST "$FLOWSTORE/submitters" \
        -H "Content-Type: application/json" \
        -d '{
            "number": 870970,
            "name": "dev-submitter",
            "description": "Local dev submitter",
            "priority": "NORMAL",
            "enabled": true
        }')
    SUBMITTER_ID=$(echo "$submitter_response" | jq -r '.id')
    echo "  Submitter ID: $SUBMITTER_ID"
fi
[[ "$SUBMITTER_ID" =~ ^[0-9]+$ ]] || die "Failed to create/find submitter"

# ── sink ───────────────────────────────────────────────────────────────────────

echo "Creating sink..."
existing_sink=$(curl -sf "$FLOWSTORE/sinks" | jq -r '.[] | select(.content.name == "dev-null-sink") | .id' 2>/dev/null || true)
if [[ -n "$existing_sink" ]]; then
    SINK_ID="$existing_sink"
    echo "  Sink already exists, ID: $SINK_ID"
else
    sink_response=$(curl -sf -X POST "$FLOWSTORE/sinks" \
        -H "Content-Type: application/json" \
        -d '{
            "name": "dev-null-sink",
            "queue": "sinkqueue::sinkqueue",
            "description": "Local dev sink — chunks are acknowledged but not delivered",
            "sinkType": "DUMMY",
            "sequenceAnalysisOption": "ID_ONLY"
        }')
    SINK_ID=$(echo "$sink_response" | jq -r '.id')
    echo "  Sink ID: $SINK_ID"
fi
[[ "$SINK_ID" =~ ^[0-9]+$ ]] || die "Failed to create/find sink"

# ── flow binders ───────────────────────────────────────────────────────────────

create_binder_if_missing() {
    local name="$1" destination="$2" flow_id="$3"

    local existing
    existing=$(curl -sf "$FLOWSTORE/binders" | jq -r --arg name "$name" '.[] | select(.content.name == $name) | .id' 2>/dev/null || true)
    if [[ -n "$existing" ]]; then
        echo "  Binder '$name' already exists, ID: $existing"
        return
    fi

    local result
    result=$(curl -sf -X POST "$FLOWSTORE/binders" \
        -H "Content-Type: application/json" \
        -d "{
            \"name\": \"$name\",
            \"description\": \"Routes to ${destination} processor\",
            \"packaging\": \"JSON\",
            \"format\": \"test\",
            \"charset\": \"utf8\",
            \"destination\": \"$destination\",
            \"priority\": \"NORMAL\",
            \"recordSplitter\": \"JSON\",
            \"flowId\": $flow_id,
            \"submitterIds\": [$SUBMITTER_ID],
            \"sinkId\": $SINK_ID,
            \"queueProvider\": null
        }")
    local binder_id
    binder_id=$(echo "$result" | jq -r '.id')
    [[ "$binder_id" =~ ^[0-9]+$ ]] || die "Failed to create binder '$name' (response: $result)"
    echo "  Binder '$name' created, ID: $binder_id"
}

echo "Creating flow binders..."
create_binder_if_missing "dev-binder-nashorn" "dev-nashorn" "$NASHORN_FLOW_ID"
create_binder_if_missing "dev-binder-graaljs" "dev-graaljs" "$GRAALJS_FLOW_ID"

echo ""
echo "Seeding complete."
echo "  Nashorn flow ID:  $NASHORN_FLOW_ID"
echo "  GraalJS flow ID:  $GRAALJS_FLOW_ID"
echo ""
echo "Use NASHORN_FLOW_ID=$NASHORN_FLOW_ID in step 5a (or re-query with:"
echo "  curl -s http://localhost:8081/dataio/flow-store-service/flows | jq '[.[] | select(.content.name == \"Passthrough\")] | .[0].id')"
