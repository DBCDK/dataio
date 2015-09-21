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

REPOS_DIR=svn.dbc.dk
TEST_SUITE=`readlink -f $1`

echo "Running test suite $TEST_SUITE"

JAIL_DIR=testarea
cd $JAIL_DIR

source ENV/bin/activate

rm -f logs.zip suite-test.log test-report.txt

suite_test $TEST_SUITE -c --verbose --pool-size 1

deactivate

cd ..

svn info $REPOS_DIR/datawell-convert/ | grep 'Last Changed Rev' | awk '{ print $4; }' > js-revision.txt

exit 0