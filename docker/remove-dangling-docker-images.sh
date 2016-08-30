#!/usr/bin/env bash
docker images -qf dangling=true | xargs docker rmi || true