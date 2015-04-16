#!/usr/bin/env python
import json
import sys

input=sys.stdin.readline()
unpacked=json.loads(input)
print(json.dumps(unpacked, indent=4, sort_keys=True))