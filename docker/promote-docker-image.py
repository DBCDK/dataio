#!/usr/bin/env python
# -*- coding: utf-8 -*-
# -*- mode: python -*-

import argparse
import os
import requests
import sys
from requests.auth import HTTPBasicAuth


def parse_args():
    parser = argparse.ArgumentParser(description='Script for promoting docker repositories')
    parser.add_argument('--registry-baseurl', default='https://docker-metascrum.artifacts.dbccloud.dk',
                        help='base URL of docker registry service')
    parser.add_argument('--username', required=True, help='registry service username')
    parser.add_argument('--password', required=True, help='registry service password')
    parser.add_argument('repository_name', help='name of docker repository including registry')
    parser.add_argument('src_tag', help='tag of repository to promote')
    parser.add_argument('target_tag', help='tag of promoted repository')
    return parser.parse_args()


def execute_http_post(url, request, username, password):
    response = requests.post(url, json=request, auth=HTTPBasicAuth(username, password))
    if response.status_code != requests.codes.OK and response.status_code != requests.codes.NOT_FOUND:
        raise Exception("error promoting repository: " + response.content)
    return response.status_code


def trim_registry(registry):
    return registry.split('.')[0]

if __name__ == "__main__":
    args = parse_args()
    (registry, repository_name) = args.repository_name.split('/')

    print "promoting %s/%s:%s to %s/%s:%s" % (registry, repository_name, args.src_tag, registry, repository_name,
                                              args.target_tag),

    request = {
        "targetRepo": trim_registry(registry),
        "dockerRepository": repository_name,
        "tag": args.src_tag,
        "targetTag": args.target_tag,
        "copy": True
    }

    print "[%s]" % (execute_http_post('/'.join([args.registry_baseurl, 'api', 'docker', trim_registry(registry), 'v2',
                                                'promote']), request, args.username, args.password))

    sys.exit(os.EX_OK)



