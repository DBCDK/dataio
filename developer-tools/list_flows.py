#!/usr/bin/env python

#
#
# python wrapper for creation of a job.
#


import json
import requests
import argparse
import sys
import time


def parse_arguments():
    global args
    parser = argparse.ArgumentParser("")
    parser.add_argument("--host", help="host til dataio systemet dataio-be-s01:8080 for staging", required=True)
    parser.add_argument("--job", help="job to show", type=int)

    args = parser.parse_args()


parse_arguments()

print()
url="http://"+args.host+"/dataio/flow-store-service/flows"

#url="http://"+args.host+"/~ja7/out.txt"

start = time.time()

response = requests.get( url )
latency = time.time() - start
if response.status_code == requests.codes.OK :
    sys.stderr.write('Elapsed : %s  - %s'%(response.elapsed, latency))
    print response.json()
else :
    print "Error from server : "+ str(response.status_code)
    print response.content
