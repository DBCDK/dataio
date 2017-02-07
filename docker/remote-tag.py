#!/usr/bin/env python
# -*- coding: utf-8 -*-
# -*- mode: python -*-

import argparse
import os
import requests
import sys
from requests.auth import HTTPBasicAuth

mime_type = 'application/vnd.docker.distribution.manifest.v2+json'


def parse_args():
    parser = argparse.ArgumentParser(description='Script for remote tagging of docker images')
    parser.add_argument('--registry-baseurl', default='https://artifactory.dbc.dk/artifactory',
                        help='base URL of docker registry service')
    parser.add_argument('--username', required=True, help='registry service username')
    parser.add_argument('--password', required=True, help='registry service password')
    parser.add_argument('repository_name', help='name of docker repository including registry')
    parser.add_argument('src_tag', help='existing tag')
    parser.add_argument('target_tag', help='new tag')
    return parser.parse_args()


def http_get_manifest(url, username, password):
    response = requests.get(url, auth=HTTPBasicAuth(username, password), headers={'Accept': mime_type})
    if response.status_code != requests.codes.OK:
        raise Exception("error GETing manifest: " + response.content)
    return response.json()


def http_put_manifest(url, manifest, username, password):
    response = requests.put(url, json=manifest, auth=HTTPBasicAuth(username, password),
                            headers={'Content-type': mime_type})
    if response.status_code != requests.codes.CREATED:
        raise Exception("error POSTing manifest: " + response.content)
    return response.status_code


def trim_registry(registry):
    return registry.split('.')[0]

if __name__ == "__main__":
    args = parse_args()
    (registry, repository_name) = args.repository_name.split('/')

    print "tagging %s/%s:%s as %s/%s:%s" % (registry, repository_name, args.src_tag, registry, repository_name,
                                            args.target_tag),

    manifest = http_get_manifest('/'.join([args.registry_baseurl, 'api', 'docker', trim_registry(registry), 'v2',
                                           repository_name, 'manifests', args.src_tag]), args.username, args.password)

    print "[%s]" % (http_put_manifest('/'.join([args.registry_baseurl, 'api', 'docker', trim_registry(registry), 'v2',
                                                repository_name, 'manifests', args.target_tag]), manifest,
                                      args.username, args.password))

    sys.exit(os.EX_OK)
