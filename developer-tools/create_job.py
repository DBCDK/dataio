#!/usr/bin/env python

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
    parser.add_argument("filename", help="datafile")
    parser.add_argument("jobspecification", help="job specification file in json")
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

fileStoreId = post_file(args.filename)
create_job(fileStoreId, load_specification(args.jobspecification))



