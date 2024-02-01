#!/bin/bash
PATH="/usr/lib/jvm/java-11/bin:${PATH}"
java -jar target/acc-test-runner.jar "$@"
