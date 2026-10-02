#!/usr/bin/env bash

mkdir -p "$1"
output_file=$1/testenv.properties
echo -n "" > "$output_file"

# plaintext diff

if [[ $(command -v grep) ]]; then
  echo testenv.grep=true >> "$output_file"
else
  echo testenv.grep=false >> "$output_file"
fi

if [[ $(command -v diff) ]]; then
  echo testenv.diff=true >> "$output_file"
else
  echo testenv.diff=false >> "$output_file"
fi

# json diff

if [[ $(command -v jq) ]]; then
  echo testenv.jq=true >> "$output_file"
else
  echo testenv.jq=false >> "$output_file"
fi

# xml diff

if [[ $(command -v cat) ]]; then
  echo testenv.cat=true >> "$output_file"
else
  echo testenv.cat=false >> "$output_file"
fi

if [[ $(command -v sed) ]]; then
  echo testenv.sed=true >> "$output_file"
else
  echo testenv.sed=false >> "$output_file"
fi

if [[ $(command -v xmllint) ]]; then
  echo testenv.xmllint=true >> "$output_file"
else
  echo testenv.xmllint=false >> "$output_file"
fi