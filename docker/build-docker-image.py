#!/usr/bin/env python
# -*- coding: utf-8 -*-
# -*- mode: python -*-

import argparse
import os
import shutil
import stat
import sys
from subprocess import check_call, STDOUT


def parse_args():
    parser = argparse.ArgumentParser(description='Script for building of dataIO docker images')
    parser.add_argument('--src-directory', required=True, help='directory containing Dockerfile')
    parser.add_argument('--build-directory', required=True, help='directory from where docker image will be built')
    parser.add_argument('--images-log', default='docker-images.log',
                        help='name of docker image built is appended to this file')
    parser.add_argument('artifact_name', help='name of artifact')
    parser.add_argument('image_name', help='name of docker image')
    return parser.parse_args()


def copy_and_overwrite(from_path, to_path):
    if os.path.exists(to_path):
        shutil.rmtree(to_path)
    shutil.copytree(from_path, to_path)


def get_basename(path):
    return os.path.basename(os.path.normpath(path))


def write_build_script(path, artifact, image_name, log):
    script_content = """#!/usr/bin/env bash

set -e
REGISTRY=docker-io.dbc.dk
NAME=%s

if [ -n "${SKIP_BUILD_DOCKER_IMAGE}" ]; then
  echo skipping building of $NAME docker image
  exit 0
fi

ARTIFACT=%s
[ -e ${ARTIFACT} ] && rm ${ARTIFACT}
ln ../${ARTIFACT} ${ARTIFACT}

TAG=${NAME}-devel
if [ -n "${BUILD_NUMBER}" ] ; then
   TAG=${REGISTRY}/${NAME}:${BUILD_NUMBER}
fi

echo building ${NAME} docker image

##
/usr/bin/time --format="time: ${NAME}-build e: %%e U: %%U S: %%S P: %%P" docker build -t ${TAG} --build-arg build_number=${BUILD_NUMBER:=devel} --build-arg svn_revision=${SVN_REVISION:=devel} -f Dockerfile .
rm ${ARTIFACT}

docker tag ${TAG} ${TAG%%:*}:latest

if [ "${BUILD_NUMBER}" != "devel" ] ; then
  echo pushing to ${REGISTRY}
  /usr/bin/time --format="time: ${NAME}-push1 e: %%e U: %%U S: %%S P: %%P " docker push ${REGISTRY}/${NAME}:${BUILD_NUMBER} &
  /usr/bin/time --format="time: ${NAME}-push2 e: %%e U: %%U S: %%S P: %%P " docker push ${REGISTRY}/${NAME}:latest &
  wait %%2
  wait %%1
  echo ${REGISTRY}/${NAME} >> %s
fi
""" % (image_name, artifact, log)
    with open(path, "w") as script_file:
        script_file.write(script_content)
    make_executable(path)


def make_executable(path):
    file_system_status = os.stat(path)
    os.chmod(path, file_system_status.st_mode | stat.S_IEXEC)


def execute_script(script_name):
    check_call(script_name, stdout=sys.stdout, stderr=STDOUT, shell=True)


if __name__ == "__main__":
    args = parse_args()
    print "src-directory=%s" % args.src_directory
    print "build-directory=%s" % args.build_directory

    return_directory = os.getcwd()
    working_directory = os.path.join(args.build_directory, get_basename(args.src_directory))

    print "return-directory=%s" % return_directory
    print "working-directory=%s" % working_directory

    print "artifact=%s" % args.artifact_name
    print "image=%s" % args.image_name

    build_script = "build_docker_image.sh"
    build_script_path = os.path.join(working_directory, build_script)

    print "build-script=%s" % build_script_path
    sys.stdout.flush()

    copy_and_overwrite(args.src_directory, working_directory)
    write_build_script(build_script_path, args.artifact_name, args.image_name, args.images_log)

    os.chdir(working_directory)
    execute_script(os.path.join(".", build_script))

    os.chdir(return_directory)
