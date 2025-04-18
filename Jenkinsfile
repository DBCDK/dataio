#!groovy

String docker_images_log_stash_tag = "docker_images_log"
String workerNode = "devel11"
Boolean DEPLOY_TO_STAGING_CANDIDATE=false
Boolean FAST_BUILD=false

pipeline {
    agent { label workerNode }
    tools {
        jdk 'jdk17'
        maven 'Maven 3'
    }
    environment {
        MAVEN_OPTS = "-Dmaven.repo.local=/home/isworker/.m2/dataio-repo -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Dorg.slf4j.simpleLogger.showThreadName=true -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"
        ARTIFACTORY_LOGIN = credentials("artifactory_login")
        GITLAB_PRIVATE_TOKEN = credentials("metascrum-gitlab-api-token")
        BUILD_NUMBER = "${env.BUILD_NUMBER}"
        DEPLOY_ARTIFACTS = "commons/utils/flow-store-service-connector, \
            commons/utils/tickle-harvester-service-connector, \
            harvester/framework, \
            gatekeeper, \
            harvester/utils/rawrepo-connector, \
            harvester/utils/harvester-job-builder, \
            commons/utils, \
            commons/utils/binary-file-store, \
            cli/acc-test-runner, \
            cli/flow-test-runner"
        SONAR_SCANNER_HOME = tool 'SonarQube Scanner from Maven Central'
        SONAR_SCANNER = "$SONAR_SCANNER_HOME/bin/sonar-scanner"
        SONAR_PROJECT_KEY = "dataio"
        SONAR_SOURCES="src"
        SONAR_TESTS="test"
    }
    triggers {
        upstream(upstreamProjects: "Docker-payara6-bump-trigger",
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
                    DEPLOY_TO_STAGING_CANDIDATE |= sh(
                            returnStatus: true,
                            script: """#!/bin/bash
                                git log -1 | tail +5 | grep -E ' *!'
                            """
                    ) == 0
                }
                script {
                    FAST_BUILD |= sh(
                            returnStatus: true,
                            script: """#!/bin/bash
                                git log -1 | tail +5 | grep -E ' *!!'
                            """
                    ) == 0
                }

            }
        }
        stage("build") {
            steps {
                sh """#!/bin/bash
                    FAST=""
                    echo FAST: ${FAST_BUILD}
                    if [ "master" != "${env.BRANCH_NAME}" ] && [ ${FAST_BUILD} -eq "true" ]; then
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

                    archiveArtifacts artifacts: "cli/acceptance-test/target/dataio-cli-acctest.jar,gatekeeper/target/dataio-gatekeeper*.jar,cli/dataio-cli",
                            fingerprint: true
                }
            }
        }
        stage("sonarqube") {
            when {
                anyOf {
                    branch "master"
                    expression { return !FAST_BUILD}
                }
            }
            steps {
                withSonarQubeEnv(installationName: 'sonarqube.dbc.dk') {
                    script {
                        // first run actual build
                        def status = 0

                        // then trigger sonarqube analysis
                        def sonarOptions = "-Dsonar.branch.name=${BRANCH_NAME}"
                        if (env.BRANCH_NAME != 'master') {
                            sonarOptions += " -Dsonar.newCode.referenceBranch=master"
                        }

                        // for java/maven projects
                        status += sh returnStatus: true, script: """
                            mvn -B $sonarOptions sonar:sonar
                        """

                        if (status != 0) {
                            error("build failed")
                        }
                    }
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
        stage("Update staging config") {
            when {
                branch "master"
            }
            steps {
                script {
                    sh """
                        java -jar buildstuff/target/buildstuff.jar version dataio.xml -t ${env.GITLAB_PRIVATE_TOKEN} -v ${env.BUILD_NUMBER}
                    """
                }
            }
        }
        stage("Update dit config") {
            when {
                branch "master"
            }
            steps {
                script {
                    sh """
                        java -jar buildstuff/target/buildstuff.jar version dataio.xml -n d -t ${env.GITLAB_PRIVATE_TOKEN} -v ${env.BUILD_NUMBER}
                    """
                }
            }
        }
        stage("Build branch artifacts for staging") {
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
        stage("Deploy branch to staging") {
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
    }
    post {
        always {
            echo 'Cleaning up'
            cleanWs()
        }
    }
}
