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


def parse_arguments():
    global args
    parser = argparse.ArgumentParser("""tool to generate harvester_config tabel from jndi values
    usage: generate_harvester_table_inserts_from_jndi_string [file] > sqlfile.sql""")
    parser.add_argument("jndi_file", help="file med jndi_value")
    args = parser.parse_args()


parse_arguments()

json_data = open(args.jndi_file).read()
unpacked = json.loads(json_data)

count = 1
version = 1
for entry in unpacked['entries']:
    entry['isEnabled'] = True
    content = {
        "type": "dk.dbc.dataio.harvester.types.RRHarvesterConfig",
        "id": count,
        "version": version,
        "content": entry,
    }

    print("insert into harvester_config (id, version , type, content ) values ( %d, %d, '%s', '%s'::jsonb);" % (
    count, version, content['type'], json.dumps(content)))
    count += 1
