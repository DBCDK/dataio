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
export CURRENT_DIR=$WORKSPACE
export MAVEN_STD_OPTIONS="-Dskip.install=false -Dmaven.repo.local=$CURRENT_DIR/.m2/repository"

touch .m2 # temporary in order to ensure that .m2 exists.

echo "Packaging .m2 into tar-file:"
time tar zcf m2.tar.gz .m2 || die "Could not tar-package .m2"

echo "Packaging all but .m2 into tar file:"
touch trunk.tar.gz # touch in order to aviod warning about changed directory from 'tar'
time tar zcf trunk.tar.gz . --exclude ".m2" --exclude "m2.tar.gz" --exclude "trunk.tar.gz" || die "Could not package trunk into tar-package"



    