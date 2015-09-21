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
import argparse
import requests
import json


def parse_arguments():
    global args
    parser = argparse.ArgumentParser("")
    parser.add_argument("jobspecification", help="job specification file in json")
    parser.add_argument("fileID", help="The Number from [urn:dataio-fs:638388] eg 638388")
    parser.add_argument("--host", help="host for the dataio system, choose dataio-be-s01:8080 for staging or dataio-be-p01:8080 for prod", required=True)

    args = parser.parse_args()


def post_file(dataFileName):
    data = open(dataFileName, 'rb')
    response = requests.post("http://" + args.host + "/dataio/file-store-service/files", data=data)

    if response.status_code == requests.codes.CREATED:
        return response.headers['location'].split("/")[-1]

    raise Exception("Error Unable to create File in fileStore")


def load_specification(specificationFileName):
    with open(specificationFileName) as json_data:
        data = json_data.read()
    return json.loads(data)


def create_job(fileId, specification):
    specification['dataFile'] = "urn:dataio-fs:" + str(fileId)
    # "dataFile": "urn:dataio-fs:" + str(fileId),
    specification = {"jobSpecification": specification, "isEndOfJob": True, "partNumber": 0}

    createJobUrl = "http://" + args.host + "/dataio/job-store-service/jobs"
    r = requests.post(createJobUrl, json.dumps(specification))

    if r.status_code == requests.codes.CREATED:
        job = json.loads(str(r.content))
        print("job " + str(job['jobId']) + " er oprettet")
        return r.headers['location']

    raise Exception("error creating job")


parse_arguments()


create_job(args.fileID, load_specification(args.jobspecification))