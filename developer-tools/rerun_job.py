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
    parser.add_argument("jobid", type=int, help="job nummeret")
    parser.add_argument("--host", help="host til dataio systemet brug dataio-be-s01:8080 for staging", required=True)
    parser.add_argument("--notify", help="Email address for job notifications", default="")

    args = parser.parse_args()


parse_arguments()

url="http://"+args.host+"/dataio/job-store-service/jobs/searches"


def get_job_specifiction(job_id):
    search_arguments={ "filtering": [ { "members": [
            {
                "filter": {
                    "field": "JOB_ID",
                    "operator": "EQUAL",
                    "value": job_id
                },
                "logicalOperator": "AND"
            }]}],
            }

    response = requests.post( url, json.dumps(search_arguments))

    if response.status_code == requests.codes.OK :
        ##print json.dumps(response.json(), indent=4, sort_keys=True)
        jsonResponse=response.json()
        return jsonResponse[0]['specification']
    else :
        print "Error from server : "+ str(response.status_code)
        print response.content
        raise Exception("Unable to get job Specification.")


def create_job(job_specification):
    job_specification['mailForNotificationAboutVerification'] = args.notify
    job_specification['mailForNotificationAboutProcessing'] = args.notify
    add_job_arguments = {"jobSpecification": job_specification, "isEndOfJob": True, "partNumber": 0}

    createJobUrl = "http://" + args.host + "/dataio/job-store-service/jobs"
    r = requests.post(createJobUrl, json.dumps(add_job_arguments))

    if r.status_code == requests.codes.CREATED:
        job = json.loads(str(r.content))
        return str(job['jobId'])

    raise Exception("error creating job")




job_specification=get_job_specifiction(args.jobid)

new_job_id=create_job( job_specification)
print("job "+ new_job_id+" er oprettet som kopi af job "+ str(args.jobid))