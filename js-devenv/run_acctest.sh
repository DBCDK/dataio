#!/bin/bash

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
