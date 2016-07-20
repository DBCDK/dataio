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


def parse_arguments():
    global args
    parser = argparse.ArgumentParser("")
    parser.add_argument("sinkId", help="sinkId")
    parser.add_argument("--host", help="host for the dataio system, choose dataio-be-s01:8080 for staging or dataio-be-p01:8080 for prod", required=True)

    args = parser.parse_args()


def post_force_bulk_mode( sinkid ):
    data={
        "sinkId": sinkid
    }
    response = requests.post("http://" + args.host + "/dataio/job-store-service/dependency/sinks/" + sinkid + "/forceBulkMode", json.dumps(data))

    print( response )



parse_arguments()

post_force_bulk_mode(args.sinkId)
