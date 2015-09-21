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
export IS_THIS_JENKINS=1 # Assume we are on Jenkins as default

# Function for exiting gracefully - kind of.
function die() {
    echo "Error:" "$@"
    exit 1
}

# Function to detect wether we operate on jenkins or in a local environment
function detectEnvironment() {
   if [[ -z "$WORKSPACE" && -z "$BUILD_NUMBER" ]]; then
       IS_THIS_JENKINS=0;
   fi
}


function setUpLocalEnvironment() {
    if [ $IS_THIS_JENKINS -eq 0 ]; then
	CURRENT_DIR=$PWD
    else
	CURRENT_DIR=$WORKSPACE
    fi
}


# Function for creating a String for building and installing maven packages 
# to local .m2, without running the integration-tests.
function createMavenPackageInstallString() {
    # To be used when there is a separate performance-test:
    # MVN_CMD="-Dskip.install=false -Dmaven.repo.local=$CURRENT_DIR/.m2/repository -Dwebdriver.firefox.bin=$CURRENT_DIR/integration-test/firefox/linux_x86_64/firefox -P -integration-test install"

    # For use while there are no separate performance-test:
    MVN_CMD="clean deploy  pmd:pmd findbugs:findbugs javadoc:aggregate -Dwebdriver.firefox.bin=$CURRENT_DIR/integration-test/firefox/linux_x86_64/firefox -Dskip.install=false -Dmaven.repo.local=$CURRENT_DIR/.m2/repository"
    
    echo $MVN_CMD
}

function getIntegrationTestsInCommaSeparatedString() {
    INTEGRATION_TEST_POMS=`find integration-test/ |grep pom.xml | egrep -v "\.svn|glassfish|postgresql" | awk -vORS=, '{ print $1 }' | sed 's/,$/\n/' | sed 's/\/pom.xml//g'`
}

# Function for creating a String for running only integration-tests in offline mode
# using packages in local .m2, without running the build/unittest/package steps of maven.
function createMavenIntegrationTestString() {
    getIntegrationTestsInCommaSeparatedString
    # cannot run offline - missing jars for failsafe-plugin
    MVN_CMD="-Dskip.install=false -Dmaven.repo.local=$CURRENT_DIR/.m2/repository -Dwebdriver.firefox.bin=$CURRENT_DIR/integration-test/firefox/linux_x86_64/firefox -P integration-test -pl $INTEGRATION_TEST_POMS  verify" 
    
    echo $MVN_CMD
}

# todo: Missing doc
function createMavenPerformanceTestParameters() {
    MVN_CMD="-Dmaven.repo.local=$CURRENT_DIR/.m2/repository -P perftest -pl performance-test -o verify" 
    
    echo $MVN_CMD
}

detectEnvironment
setUpLocalEnvironment

function getBuildNumberOfLastStableDataIOBuild() {
    wget http://is.dbc.dk/job/dataio/lastStableBuild/buildNumber -O lastStableDataIOBuildNumber.txt
    LAST_STABLE_DATAIO_BUILD_NUMBER=`cat lastStableDataIOBuildNumber.txt`
    echo "Last stable: $LAST_STABLE_DATAIO_BUILD_NUMBER"
}

function copyM2TarFromLastStableBuild() {
    echo "Copy m2.tar.gz from last successful build:"
    rm m2.tar.gz*
    time wget --progress dot:giga http://is.dbc.dk/job/dataio/$LAST_STABLE_DATAIO_BUILD_NUMBER/artifact/m2.tar.gz -O m2.tar.gz || die "Could not download m2.tar.gz from last successfull build"
}

function unpackageM2() {
    echo "Unpackage .m2 from tar-file:"
    time tar zxf m2.tar.gz || die "Could not un tar-package m2.tar.gz"
}

function deleteDBCJarsFromM2() {
    echo "Delete dk-jars from local .m2"
    rm -rf .m2/repository/dk/ || die "Could not delete dk-jars from local .m2"
}

function packageM2() {
    touch .m2 # temporary in order to ensure that .m2 exists.
    echo "Packaging .m2 into tar-file:"
    time tar zcf m2.tar.gz .m2 || die "Could not tar-package .m2"
}

function packageCompiledTrunk() {
    echo "Packaging all but .m2 into tar file:"
    touch trunk.tar.gz # touch in order to aviod warning about changed directory from 'tar'
    time tar zcf trunk.tar.gz . --exclude ".m2" --exclude "m2.tar.gz" --exclude "trunk.tar.gz" || die "Could not package trunk into tar-package"
}

function buildPreStep() {
    getBuildNumberOfLastStableDataIOBuild
    copyM2TarFromLastStableBuild
    unpackageM2
    deleteDBCJarsFromM2
    rm lastStableDataIOBuildNumber.txt
    rm m2.tar.gz
}

function buildPostStep() {
    packageM2
    packageCompiledTrunk
}

function testingPreStep() {
    getBuildNumberOfLastStableDataIOBuild
    copyM2TarFromLastStableBuild
    unpackageM2
}

function validateAndGetParameterString() {
    legal_parameters="build integrationtest performancetest"
    [[ $legal_parameters =~ $1 ]] && echo "" || echo "$1 is an illegal parameter. Legal parameters are : $legal_parameters"

    case $1 in
	build)
	    createMavenPackageInstallString
	    ;;
	integrationtest)
	    createMavenIntegrationTestString
	    ;;
	performancetest)
	    createMavenPerformanceTestParameters
	    ;;
    esac
}

function validateAndPerformState() {
    legal_parameters="pre-build post-build pre-integrationtest pre-performancetest"
    [[ $legal_parameters =~ $1 ]] && echo "" || echo "$1 is an illegal parameter. Legal parameters are : $legal_parameters"

    case $1 in
	pre-build)
	    buildPreStep
	    ;;
	post-build)
	    buildPostStep
	    ;;
    esac
}

while getopts ":p:s:i" optname
do
    case "$optname" in
	"p")
	    # Retriving a parameter string for $OPTARG
	    validateAndGetParameterString $OPTARG
	    ;;
	"s")
	    validateAndPerformState $OPTARG
	    ;;
	"i")
	    getIntegrationTestsInCommaSeparatedString
	    echo $INTEGRATION_TEST_POMS
	    ;;
	":")
	    echo "No argument value for $OPTARG"
	    ;;
	"?")
	    echo "Unknown option $OPTARG"
	    ;;
	*)
	    echo "This should not happen"
	    ;;
    esac
done

# --getMavenParams build / integrationtest / performancetest

# get version of last stable dataio build
# copy .m2 from last stable dataio (from version)
# 