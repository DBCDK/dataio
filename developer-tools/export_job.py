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
import json
import requests
import argparse
from urlparse import urlparse


def parse_arguments():
    global args
    parser = argparse.ArgumentParser("Exports the datafile for a dataIO job")
    parser.add_argument("jobid", help="job nummeret")
    parser.add_argument("filenamePrefix", help="Filename prefix - ie. mitSejeExportJob becomes mitSejeExportJob.data og mitSejeExportJob.specification")
    parser.add_argument("--dataio-instance",
                        help="dataio system instance, e.g. dataio.dbc.dk (default) or dataio-staging.dbc.dk",
                        default="dataio.dbc.dk")
    parser.add_argument("--jobstorehost",
                        help="jobstore host for the dataio system, overrides value reported by dataio instance")
    parser.add_argument("--filestorehost",
                        help="filestore host for the dataio system, overrides value reported by dataio instance")

    args = parser.parse_args()


def resolve_hosts():
    response = requests.get("http://" + args.dataio_instance + "/urls")
    if response.status_code == requests.codes.OK:
        urls = json.loads(response.content)
        if args.filestorehost is None:
            args.filestorehost = urlparse(urls['url/dataio/filestore/rs']).hostname
        if args.jobstorehost is None:
            args.jobstorehost = urlparse(urls['url/dataio/jobstore/rs']).hostname
        return

    print response.content
    raise Exception("error resolving hosts")


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

    job_searches_url = "http://" + args.jobstorehost + "/dataio/job-store-service/jobs/searches"
    response = requests.post(job_searches_url, json.dumps(search_arguments))

    if response.status_code == requests.codes.OK:
        # print json.dumps(response.json(), indent=4, sort_keys=True)
        json_response = response.json()
        return json_response[0]['specification']
    else:
        print "Error from server : " + job_id + "\n" + str(response.status_code)
        print response.content
        raise Exception("Unable to get job specification.")


def download_from_file_store(file_no, filename):
    response = requests.get("http://" + args.filestorehost + "/dataio/file-store-service/files/" + str(file_no))
    chunk_size = 16 * 4 * 1024
    with open(filename, 'wb') as fd:
        for chunk in response.iter_content(chunk_size):
            fd.write(chunk)


def write_json_to_file(json_content, filename):
    with open(filename, 'wb') as fd:
        json.dump(json_content, fd, indent=4, sort_keys=True)


def get_file_store_number(job_specification):
    return job_specification['dataFile'].split(":")[-1]

parse_arguments()
resolve_hosts()

print("getting data from job " + args.jobid)
job_specification = get_job_specifiction(args.jobid)
file_no = get_file_store_number(job_specification)

print(json.dumps(job_specification))
print("datafile no " + file_no)

download_from_file_store(file_no, args.filenamePrefix + ".data")
write_json_to_file(job_specification, args.filenamePrefix + ".specification")

print("job " + args.jobid + " exported to :")
print("    " + args.filenamePrefix + ".specification")
print("    " + args.filenamePrefix + ".data")
