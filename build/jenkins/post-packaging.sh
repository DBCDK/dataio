#!/bin/bash

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



    

