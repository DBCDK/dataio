#!/usr/bin/env python

# DataIO - Data IO
# Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
# Denmark. CVR: 15149043
#
# This file is part of DataIO.
#
# DataIO is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# DataIO is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with DataIO.  If not, see <http://www.gnu.org/licenses/>.


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
    parser.add_argument("--submitter", help="only show for a given submitter")
    parser.add_argument("--count", help="count the jobs serverside", dest='count', action='store_const', default=False, const=True)

    args = parser.parse_args()


parse_arguments()

url="http://"+args.host+"/dataio/job-store-service/jobs/searches"
if args.count :
    url=url+"/count"


search_arguments={
   "limit": args.limit, "offset": args.offset,
   "ordering": [
      {
      "field": "JOB_ID",
      "sort": "DESC"
      }
      ]
   }

if args.submitter :
    search_arguments['filtering'] = [{"members":
        [{
            "filter": {
                "field": "SPECIFICATION",
                "operator": "JSON_LEFT_CONTAINS",
                "value": '{ "submitterId": %s}'%args.submitter
            },
            "logicalOperator": "AND",
        }
        ]}]


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