#!/usr/bin/env bash
set -eo pipefail

usage() {
  cat <<EOF
Publishes docker images from  **/target/docker.out files.
Only on CI server.
EOF
}

# -------------------- Args --------------------
while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help) usage; exit 0;;
    *) echo "Unknown arg: $1"; usage; exit 1;;
  esac
done

function is_ci_server() {
  if [[ -n "$JENKINS_URL" && -n "$BUILD_NUMBER" && -n "$BRANCH_NAME" ]]; then
    return 0  # true
  else
    return 1  # false
  fi
}

function for_each_docker_out() {
  local any_found=1 # 1 = false, 0 = true (shell-style)
  while IFS= read -r -d '' f; do
    any_found=0
    "$@" "$f"
  done < <(find . -type f -path '*/target/docker.out' -print0)

  return "$any_found"
}

function push_from_file() {
  local f="$1"
  while IFS= read -r image_ref || [[ -n "$image_ref" ]]; do
    [[ -z "$image_ref" ]] && continue
    echo "docker push $image_ref  (from $f)"
    docker push "$image_ref"
  done < "$f"
}

function print_file() {
  local f="$1"
  echo "----- $f -----"
  cat "$f"
}

if is_ci_server; then
  echo "CI detected: pushing docker images from */target/docker.out files."
  if ! for_each_docker_out push_from_file; then
    echo "No */target/docker.out files found."
    exit 0
  fi
else
  echo "Not on CI: printing docker.out content from */target/docker.out files."
  if ! for_each_docker_out print_file; then
    echo "No */target/docker.out files found."
    exit 0
  fi
fi
