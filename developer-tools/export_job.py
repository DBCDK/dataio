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
    parser.add_argument("jobid", help="job nummeret")
    parser.add_argument("filenamePrefix", help="Filename perfix")
    parser.add_argument("--host", help="host til dataio systemet brug dataio-be-s01:8080 for staging", required=True)

    args = parser.parse_args()


parse_arguments()

url = "http://" + args.host + "/dataio/job-store-service/jobs/searches"


def get_job_specifiction(job_id):
    search_arguments = {"filtering": [{"members": [
        {
            "filter": {
                "field": "JOB_ID",
                "operator": "EQUAL",
                "value": int(job_id)
            },
            "logicalOperator": "AND"
        }]}],
    }

    response = requests.post(url, json.dumps(search_arguments))

    if response.status_code == requests.codes.OK:
        ##print json.dumps(response.json(), indent=4, sort_keys=True)
        jsonResponse = response.json()
        return jsonResponse[0]['specification']
    else:
        print "Error from server : " + job_id + "\n" + str(response.status_code)
        print response.content
        raise Exception("Unable to get job Specifciatoin.")


def create_job(job_specification):
    add_job_arguments = {"jobSpecification": job_specification, "isEndOfJob": True, "partNumber": 0}

    createJobUrl = "http://" + args.host + "/dataio/job-store-service/jobs"
    r = requests.post(createJobUrl, json.dumps(add_job_arguments))

    if r.status_code == requests.codes.CREATED:
        job = json.loads(str(r.content))
        return str(job['jobId'])

    raise Exception("error creating job")


def down_load_file_from_file_store(file_no, filename):
    r = requests.get("http://" + args.host + "/dataio/file-store-service/files/" + str(file_no))
    chunk_size = 16 * 4 * 1024
    with open(filename, 'wb') as fd:
        for chunk in r.iter_content(chunk_size):
            fd.write(chunk)
    fd.close()


print("getting data from job " + args.jobid)
job_specification = get_job_specifiction(args.jobid)
file_no = job_specification['dataFile'].split(":")[-1]

print(json.dumps(job_specification))

print("datafile no " + file_no)
down_load_file_from_file_store(file_no, args.filenamePrefix + ".data")
with  open(args.filenamePrefix + ".specification", 'wb') as fd:
    json.dump(job_specification, fd, indent=4, sort_keys=True)

print("job " + args.jobid + " exported to :")
print("    " + args.filenamePrefix + ".specification")
print("    " + args.filenamePrefix + ".data")
