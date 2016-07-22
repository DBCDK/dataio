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
import json
import requests


sinksUpdaded=[];
def parse_arguments():
    global args
    parser = argparse.ArgumentParser("")
    parser.add_argument("sinkId", help="sinkId use [all] for all sinks")
    parser.add_argument("--host", help="host for the dataio system, choose dataio-be-s01:8080 for staging or dataio-be-p01:8080 for prod", required=True)

    args = parser.parse_args()


def post_force_bulk_mode( sinkid ):
    data={
        "sinkId": sinkid
    }
    response = requests.post("http://" + args.host + "/dataio/job-store-service/dependency/sinks/" + str(sinkid)+ "/forceBulkMode", json.dumps(data))
    if response.status_code == requests.codes.OK :
        sinksUpdaded.append( sinkid)
    else :
        print "Error for sink ", sinkid, response

parse_arguments()

def getSinkId( arg) :
    return arg['id']

def get_all_sinkIds( ) :
    response = requests.get( "http://" + args.host + "/dataio/flow-store-service/sinks/")

    return response.json()


if args.sinkId=="all" :
    for sink in get_all_sinkIds():
        post_force_bulk_mode( getSinkId( sink ))
else :
    post_force_bulk_mode(args.sinkId)

print "sinks set to bulkMode ", sinksUpdaded