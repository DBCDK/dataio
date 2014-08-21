#!/bin/bash -x

REPOS_URL=https://svn.dbc.dk/repos
REPOS_DIR=svn.dbc.dk

isNotSet() {
    name=$1
    eval value=\$$name
    if [ -z "$value" ]
        then
            return 0
        else
            return 1
    fi
}


if isNotSet JOB_NAME ; then
     JOB_NAME=dataio-js-acceptance-test-VERIFIED
fi
echo $JOB_NAME
if isNotSet BUILD_NUMBER ; then
     BUILD_NUMBER=`curl --silent http://is.dbc.dk/job/$JOB_NAME/lastSuccessfulBuild/buildNumber`
fi
echo $BUILD_NUMBER

if [ ! -d "$REPOS_DIR" ]; then
    svn co --depth empty $REPOS_URL $REPOS_DIR
    cd $REPOS_DIR
    svn up jsshell-acctest
    svn up datawell-convert
    cd ..
fi

JAIL_DIR=testarea
rm -rf $JAIL_DIR
mkdir $JAIL_DIR
cd $JAIL_DIR
BUILDSCRIPT_DIR=/usr/local/lib/is.dbc.dk

# Start port allocator
BUILD_ID=dontKillMe $BUILDSCRIPT_DIR/port_allocator_service_daemon.py

VENV=`pwd`/ENV
EGGS=`pwd`/dependencies
rm -f dependencies.txt
rm -f $EGGS/*

DEPENDENCY_MANAGER=$BUILDSCRIPT_DIR/dependency_manager/bin/dependency-manager
$DEPENDENCY_MANAGER $JOB_NAME $BUILD_NUMBER
$DEPENDENCY_MANAGER $JOB_NAME $BUILD_NUMBER -a testrunner-jsshell-convert --verbose
$DEPENDENCY_MANAGER --download $EGGS --pattern ".*\.(egg)$" --verbose

$BUILDSCRIPT_DIR/envbuilder.sh -v -s -f -e $VENV $EGGS
cd ..

exit 0
