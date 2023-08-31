#!groovy

String docker_images_log_stash_tag = "docker_images_log"
String workerNode = "devel11"
Boolean DEPLOY_TO_STAGING_CANDIDATE=false

pipeline {
    agent {label workerNode}
    tools {
		jdk 'jdk11'
		maven 'Maven 3'
    }
    environment {
        MAVEN_OPTS="-Dmaven.repo.local=/home/isworker/.m2/dataio-repo -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Dorg.slf4j.simpleLogger.showThreadName=true -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"
        ARTIFACTORY_LOGIN = credentials("artifactory_login")
        GITLAB_PRIVATE_TOKEN = credentials("metascrum-gitlab-api-token")
        BUILD_NUMBER="${env.BUILD_NUMBER}"
        DEPLOY_ARTIFACTS="commons/utils/flow-store-service-connector, \
            commons/utils/tickle-harvester-service-connector, \
            gatekeeper, \
            job-processor2, \
            dlq-errorhandler, \
            sink/dummy, \
            sink/marcconv, \
            sink/dmat, \
            sink/worldcat, \
            sink/diff, \
            sink/holdings-items, \
            sink/vip, \
            sink/ims, \
            sink/tickle-repo, \
            sink/openupdate, \
            sink/batch-exchange, \
            sink/periodic-jobs, \
            sink/dpf, \
            harvester/corepo, \
            commons/utils/binary-file-store, \
            job-store-service/war \
            "
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
                            returnStdout: true,
                            script: """#!/bin/bash
                                git log -1 | tail +5 | grep -E ' *!' | echo ""
                            """
                    ).trim().isEmpty()
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
                    mvn -B -T 4 \${FAST} -Dtag="${env.BRANCH_NAME}-${env.BUILD_NUMBER}" install
                    test -n \${FAST} && mvn -B -T 6 -P !integration-test pmd:pmd
                    echo Build CLI for \$BRANCH_NAME \$BUILD_NUMBER
                    ./cli/build_docker_image.sh
                """
                script {
                    junit allowEmptyResults: true, testResults: '**/target/*-reports/*.xml'

                    def java = scanForIssues tool: [$class: 'Java']
                    publishIssues issues: [java], unstableTotalAll: 1

                    def pmd = scanForIssues tool: [$class: 'Pmd']
                    publishIssues issues: [pmd], unstableTotalAll: 1

                    def spotbugs = scanForIssues tool: [$class: 'SpotBugs']
                    publishIssues issues: [spotbugs], unstableTotalAll: 1

                    archiveArtifacts artifacts: "docker-images.log,cli/acceptance-test/target/dataio-cli-acctest.jar,gatekeeper/target/dataio-gatekeeper*.jar,cli/dataio-cli",
                            fingerprint: true
                }
            }
        }
        stage("docker push") {
            when {
                branch "master"
            }
            steps {
                sh """
                cat docker-images.log | parallel -j 3 docker push {}:master-${env.BUILD_NUMBER}
                docker tag docker-metascrum.artifacts.dbccloud.dk/gatekeeper-jmx-exporter:devel docker-metascrum.artifacts.dbccloud.dk/gatekeeper-jmx-exporter:DIT-${env.BUILD_NUMBER}
                docker push docker-metascrum.artifacts.dbccloud.dk/gatekeeper-jmx-exporter:DIT-${env.BUILD_NUMBER}
            """
                script {
                    stash includes: "docker-images.log", name: docker_images_log_stash_tag
                    archiveArtifacts "docker-images.log"
                }
            }
        }
        stage("deploy to mavenrepo.dbc.dk") {
            when {
                branch "master"
            }
            steps {
                sh """
                mvn deploy -T 6 -B -Dmaven.test.skip=true -Pdocker-push -am -pl "${DEPLOY_ARTIFACTS}"
            """
            }
        }
        stage("promote to DIT") {
            when {
                branch "master"
            }
            steps {
                dir("docker") {
                    unstash docker_images_log_stash_tag
                    sh """
                    cat docker-images.log | parallel -j 3 docker tag {}:master-${env.BUILD_NUMBER} {}:DIT-${env.BUILD_NUMBER}
                    cat docker-images.log | parallel -j 3 docker push {}:DIT-${env.BUILD_NUMBER}
                """
                }
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
        stage("bump docker tags in dataio-secrets") {
            agent {
                docker {
                    label workerNode
                    image "docker-dbc.artifacts.dbccloud.dk/build-env:latest"
                    alwaysPull true
                }
            }
            when {
                branch "master"
            }
            steps {
                script {
                    sh """
                    set-new-version services ${env.GITLAB_PRIVATE_TOKEN} metascrum/dataio-secrets DIT-${env.BUILD_NUMBER} -b staging
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
                branch "master"
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
                        mvn deploy -B -T 6 -Dmaven.test.skip=true -Pdocker-push -Dtag="${env.BRANCH_NAME}-${env.BUILD_NUMBER}" -am -pl "${DEPLOY_ARTIFACTS}"
                        cat docker-images.log | parallel -j 3  docker push {}:${env.BRANCH_NAME}-${env.BUILD_NUMBER}
                """
                    }
                }
            }
        }
        stage("bump docker tags in dataio-secrets for non-master branches") {
            agent {
                docker {
                    label workerNode
                    image "docker-dbc.artifacts.dbccloud.dk/build-env:latest"
                    alwaysPull true
                }
            }
            when {
                not {
                    branch "master"
                }

            }
            steps {
                script {
                    if (env.DEPLOY_TO_STAGING_CANDIDATE.toBoolean()) {
                        sh """
                            echo "Gogo version gadget!!!"
                            set-new-version services ${env.GITLAB_PRIVATE_TOKEN} metascrum/dataio-secrets ${env.BRANCH_NAME}-${env.BUILD_NUMBER} -b staging
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
