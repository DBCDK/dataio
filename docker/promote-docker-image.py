#!/usr/bin/env python
# -*- coding: utf-8 -*-
# -*- mode: python -*-

import argparse
import requests
from requests.auth import HTTPBasicAuth


def parse_args():
    parser = argparse.ArgumentParser(description='Script for promoting docker images')
    parser.add_argument('--registry-baseurl', default='https://artifactory.dbc.dk/artifactory',
                        help='base URL of docker registry service')
    parser.add_argument('--src', default="docker-io", help='source registry')
    parser.add_argument('--target', default="docker-io", help='target registry')
    parser.add_argument('--username', required=True, help='registry service username')
    parser.add_argument('--password', required=True, help='registry service password')
    parser.add_argument('repository_name', help='name of docker repository')
    parser.add_argument('src_tag', help='tag of source repository')
    parser.add_argument('target_tag', help='tag of target repository')
    return parser.parse_args()


def execute_http_post(url, request, username, password):
    response = requests.post(url, json=request, auth=HTTPBasicAuth(username, password))
    if response.status_code != requests.codes.OK:
        raise Exception("error promoting repository: " + response.content)

if __name__ == "__main__":
    args = parse_args()
    print "promoting %s/%s/%s to %s/%s/%s" % (args.src, args.repository_name, args.src_tag, args.target, args.repository_name,
                                              args.target_tag)

    request = {
        "targetRepo": args.target,
        "dockerRepository": args.repository_name,
        "tag": args.src_tag,
        "targetTag": args.target_tag,
        "copy": True
    }

    execute_http_post('/'.join([args.registry_baseurl, 'api', 'docker', args.src, 'v2', 'promote']), request,
                      args.username, args.password)




