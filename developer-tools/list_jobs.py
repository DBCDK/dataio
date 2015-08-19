#!/usr/bin/env python

#
#
# python wrapper for creation of a job.
#


import json
import requests
import argparse


def parse_arguments():
    global args
    parser = argparse.ArgumentParser("")
    parser.add_argument("--host", help="host til dataio systemet dataio-be-s01:8080 for staging", required=True)
    parser.add_argument("--job", help="job to show", type=int)
    parser.add_argument("--limit", help="limit number of jobs to list", type=int, default=0)
    parser.add_argument("--offset", help="start from offset ", type=int, default=0)

    args = parser.parse_args()


parse_arguments()

print()
url="http://"+args.host+"/dataio/job-store-service/jobs/searches"

search_arguments={ "limit": args.limit, "offset": args.offset, "ordering": [
{
   "field": "JOB_ID",
   "sort": "DESC"
   }
   ]
   }

search_on_id={ "filtering": [ { "members": [
            {
                "filter": {
                    "field": "JOB_ID",
                    "operator": "EQUAL",
                    "value": 0
                },
                "logicalOperator": "AND"
            }]}],
            }

if  args.job :
    print("jobs id : %d"%(args.job))

    search_on_id['filtering'][0]['members'][0]['filter']['value']=args.job
    search_arguments = search_on_id


response = requests.post( url, json.dumps(search_arguments))

if response.status_code == requests.codes.OK :
    print json.dumps(response.json(), indent=4, sort_keys=True)
else :
    print "Error from server : "+ str(response.status_code)
    print response.content
