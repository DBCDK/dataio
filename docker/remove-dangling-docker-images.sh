#!/usr/bin/env bash
docker images -qf dangling=true | xargs --no-run-if-empty docker rmi