#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# -*- mode: python -*-

import argparse
import os
import shutil
import stat
import sys


def parse_args():
    parser = argparse.ArgumentParser(description='Creates dataIO docker image build script')
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


def write_build_script(path, image_name, log):
    script_content = """#!/usr/bin/env bash

set -e
REGISTRY=docker-metascrum.artifacts.dbccloud.dk
NAME=%s
TIMEFORMAT="time: ${NAME} e: %%E U: %%U S: %%S P: %%P "

if [[ -n "${SKIP_BUILD_DOCKER_IMAGE}" ]]; then
  echo skipping building of ${NAME} docker image
  exit 0
fi

RETURN_DIR="$(pwd)"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}/.."

TAG=devel
if [ -n "${BUILD_NUMBER}" ] ; then
  TAG=${BUILD_NUMBER}
  test ! -z ${BRANCH_NAME} && TAG=${BRANCH_NAME}-${TAG}
fi
IMAGE=${REGISTRY}/${NAME}:${TAG}

echo building ${IMAGE} docker image

##
time docker build -t ${IMAGE} --build-arg build_number=${BUILD_NUMBER:=devel} --build-arg git_commit=${GIT_COMMIT:=devel} -f docker/Dockerfile --pull --no-cache .

echo ${REGISTRY}/${NAME} >> %s

cd "${RETURN_DIR}"
""" % (image_name, log)
    with open(path, "w") as script_file:
        script_file.write(script_content)
    make_executable(path)


def make_executable(path):
    file_system_status = os.stat(path)
    os.chmod(path, file_system_status.st_mode | stat.S_IEXEC)


if __name__ == "__main__":
    print(sys.argv[1:])

    args = parse_args()
    print("src-directory=%s" % args.src_directory)
    print("build-directory=%s" % args.build_directory)

    working_directory = os.path.join(args.build_directory, get_basename(args.src_directory))

    print("working-directory=%s" % working_directory)

    print("artifact=%s" % args.artifact_name)
    print("image=%s" % args.image_name)

    build_script = "build_docker_image.sh"
    build_script_path = os.path.join(working_directory, build_script)

    print("build-script=%s" % build_script_path)
    sys.stdout.flush()

    write_build_script(build_script_path, args.image_name, args.images_log)
