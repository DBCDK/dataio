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

start = time.time()

response = requests.get( url )
latency = time.time() - start
if response.status_code == requests.codes.OK :
    sys.stderr.write('Elapsed : %s  - %s'%(response.elapsed, latency))
    print response.json()
else :
    print "Error from server : "+ str(response.status_code)
    print response.content