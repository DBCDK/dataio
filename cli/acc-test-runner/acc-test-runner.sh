#!/bin/bash
PATH="/usr/lib/jvm/java-11/bin:${PATH}"
mvn dependency:get -Dtransitive=false -B -Dartifact=dk.dbc:acc-test-runner:2.0-SNAPSHOT -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -Ddest=. -DrepoUrl=https://mavenrepo.dbc.dk/content/groups/public/
java -jar acc-test-runner.jar "$@"
