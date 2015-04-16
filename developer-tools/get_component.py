#!/usr/bin/env python3

import json
import requests
import argparse

parser = argparse.ArgumentParser("")
parser.add_argument("--host", help="hostname:port eks. dataio-be-s01:1080")
parser.add_argument("--componentid", help="fisk", required=True)
args=parser.parse_args()

r = requests.get("http://"+args.host + "/dataio/flow-store-service/components/" + args.componentid)

print(json.dumps(r.json(), indent=4, sort_keys=True))
