#!/usr/bin/env python3

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

parser = argparse.ArgumentParser("")
parser.add_argument("--host", help="hostname:port eks. dataio-be-s01:1080")
parser.add_argument("--componentid", help="fisk", required=True)
args=parser.parse_args()

r = requests.get("http://"+args.host + "/dataio/flow-store-service/components/" + args.componentid)

print(json.dumps(r.json(), indent=4, sort_keys=True))