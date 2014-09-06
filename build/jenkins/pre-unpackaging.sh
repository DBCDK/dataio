#!/bin/bash

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
