#!/bin/bash
#
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

function die() {
    echo "Error:" "$@"
    exit 1
}

# Detect environment: jenkins/local

# Set constants according to environment

echo "Copy m2.tar.gz from last successful build:"
time wget http://is.dbc.dk/job/dataio/lastStableBuild/artifact/m2.tar.gz || die "Could not download m2.tar.gz from last successfull build"

echo "Unpackage .m2 from tar-file:"
time tar zxf m2.tar.gz || die "Could not un tar-package m2.tar.gz"

echo "Delete dk-jars from local .m2"
rm -rf .m2/repository/dk/ || die "Could not delete dk-jars from local .m2"