#!groovy

String docker_images_log_stash_tag = "docker_images_log"
String workerNode = "devel11"
Boolean DEPLOY_TO_STAGING_CANDIDATE=false
//Byg!!
pipeline {
    agent {label workerNode}
    tools {
		jdk 'jdk17'
		maven 'Maven 3'
    }
    environment {
        MAVEN_OPTS="-Dmaven.repo.local=/home/isworker/.m2/dataio-repo -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Dorg.slf4j.simpleLogger.showThreadName=true -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"
        ARTIFACTORY_LOGIN = credentials("artifactory_login")
        GITLAB_PRIVATE_TOKEN = credentials("metascrum-gitlab-api-token")
        BUILD_NUMBER="${env.BUILD_NUMBER}"
        DEPLOY_ARTIFACTS="commons/utils/flow-store-service-connector, \
            commons/utils/tickle-harvester-service-connector, \
            harvester/framework, \
            gatekeeper, \
            harvester/utils/rawrepo-connector, \
            harvester/utils/harvester-job-builder, \
            commons/utils, \
            commons/utils/binary-file-store, \
            cli/acc-test-runner"
    }
    triggers {
        upstream(upstreamProjects: "Docker-payara5-bump-trigger",
			threshold: hudson.model.Result.SUCCESS)
    }
    options {
        skipDefaultCheckout(true)
        buildDiscarder(logRotator(artifactDaysToKeepStr: "",
            artifactNumToKeepStr: "", daysToKeepStr: "30", numToKeepStr: "30"))
        timestamps()
        timeout(time: 1, unit: "HOURS")
        disableConcurrentBuilds(abortPrevious: true)
        lock('dataio-build')
    }
    stages {
        stage('clean and checkout') {
            steps {
                cleanWs()
                sh """
                    test -d /home/isworker/.m2/dataio-repo/dk/dbc && rm -r /home/isworker/.m2/dataio-repo/dk/dbc || echo ok
                """
                checkout scm
                script {
                    DEPLOY_TO_STAGING_CANDIDATE|=sh(
                            returnStatus: true,
                            script: """#!/bin/bash
                                git log -1 | tail +5 | grep -E ' *!'
                            """
                    ) == 0
                }
            }
        }
        stage("build") {
            steps {
                sh """#!/bin/bash
                    FAST=""
                    if [ "master" != "${env.BRANCH_NAME}" ] && [ -n "\$(git log -1 | tail +5 | grep -E ' *!!')" ]; then
                        echo Fast branch deployment skip all tests
                        FAST=" -P !integration-test -Dmaven.test.skip=true "
                    fi
                    mvn -B -T 4 \${FAST} -Dtag="${env.BRANCH_NAME}-${env.BUILD_NUMBER}" install || exit 1
                    if [ -n "\${FAST}" ]; then
                        echo Run PMD 
                        mvn -B -T 6 -P !integration-test pmd:pmd
                    fi
                """
                script {
                    junit allowEmptyResults: true, testResults: '**/target/*-reports/*.xml'

                    def java = scanForIssues tool: [$class: 'Java']
                    publishIssues issues: [java], unstableTotalAll: 10

                    def pmd = scanForIssues tool: [$class: 'Pmd']
                    publishIssues issues: [pmd], unstableTotalAll: 1

                    def spotbugs = scanForIssues tool: [$class: 'SpotBugs']
                    publishIssues issues: [spotbugs], unstableTotalAll: 1
                    archiveArtifacts artifacts: "cli/acceptance-test/target/dataio-cli-acctest.jar,gatekeeper/target/dataio-gatekeeper*.jar,cli/dataio-cli",
                            fingerprint: true
                }
            }
        }
        stage("deploy to mavenrepo.dbc.dk") {
            when {
                branch "master"
            }
            steps {
                sh """
                mvn install -T 3 -B -Dmaven.test.skip=true -Pdocker-push
                mvn deploy -T 6 -B -Dmaven.test.skip=true -Ddocker.skip=true -pl "${DEPLOY_ARTIFACTS}" -am
            """
            }
        }
        stage("Clean up docker images") {
            when {
                branch "master"
            }
            steps {
                sh """
                ./docker/remove-images docker-metascrum.artifacts.dbccloud.dk/dbc-payara-*
                ./docker/remove-images docker-metascrum.artifacts.dbccloud.dk/dataio-*
            """
            }
        }
        stage("bump version in dataio-secrets") {
            when {
                branch "master"
            }
            steps {
                script {
                    sh """
                        java -jar buildstuff/target/buildstuff.jar version dataio.xml -t ${env.GITLAB_PRIVATE_TOKEN} -v DIT-${env.BUILD_NUMBER}
                    """
                }
            }
        }
        stage("bump docker tags in dit-gitops-secrets") {
            agent {
                docker {
                    label workerNode
                    image "docker-dbc.artifacts.dbccloud.dk/build-env:latest"
                    alwaysPull true
                }
            }
            when {
                branch "DO_NOT_BUMP_DIT_GITOPS_UNTIL_IMAGES_NAMES_ARE_IN_PLACE"
            }
            steps {
                script {
                    sh """
                    set-new-version services/dataio-project ${env.GITLAB_PRIVATE_TOKEN} metascrum/dit-gitops-secrets DIT-${env.BUILD_NUMBER} -b master
                """
                }
            }
        }
        stage("deploy this branch to staging? (then push dockers to artifactory first)") {
            when {
                not {
                    branch "master"
                }
            }
            steps {
                script {
                    if (DEPLOY_TO_STAGING_CANDIDATE) {
                        sh """
                            echo "Gogo staging gadget!!!"
                            mvn install -B -T 3 -Dmaven.test.skip=true -Pdocker-push -Dtag="${env.BRANCH_NAME}-${env.BUILD_NUMBER}"
                            mvn deploy -T 6 -B -Dmaven.test.skip=true -Ddocker.skip=true -pl "${DEPLOY_ARTIFACTS}"
                        """
                    }
                }
            }
        }
        stage("bump docker tags in dataio-secrets for non-master branches") {
            when {
                not {
                    branch "master"
                }

            }
            steps {
                script {
                    if (DEPLOY_TO_STAGING_CANDIDATE) {
                        sh """
                            echo "Gogo version gadget!!!"
                            java -jar buildstuff/target/buildstuff.jar version dataio.xml -t ${env.GITLAB_PRIVATE_TOKEN} -v ${env.BRANCH_NAME}-${env.BUILD_NUMBER}
                    """
                    }
                }
            }
        }
//    post {
//        always {
//            echo 'Cleaning up'
//            cleanWs()
//        }
    }
}
